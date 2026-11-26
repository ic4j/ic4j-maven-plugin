#!/usr/bin/env bash
set -euo pipefail
: "${RUNNER_TEMP:?}" "${GITHUB_WORKSPACE:?}" "${CANDID_REV:?}" "${AGENT_REV:?}" "${MANAGEMENT_REV:?}"
cd "$GITHUB_WORKSPACE"
work="$RUNNER_TEMP/ic4j-maven-plugin-ci"
repo="$work/maven"
reports="$work/reports"
mkdir "$work"
mkdir "$repo" "$reports"
for project in candid agent management plugin; do
    sha="$(git -C "$project" rev-parse HEAD)"
    if [[ "$project" != plugin ]]; then
        pin="${project^^}_REV"
        [[ "$sha" == "${!pin}" ]]
    fi
    printf '%s %s\n' "$project" "$sha"
done | tee "$reports/sources.txt"
mvn -v | tee "$reports/maven-version.txt"

# Retry only fresh dependency downloads that received HTTP 429.
# Never retry a stateful test against a partly installed canister.
build() {
    local project="$1" attempt rc
    shift
    for attempt in 1 2 3; do
        if "$@" 2>&1 | tee "$reports/$project-$attempt.log"; then
            return 0
        else
            rc=$?
        fi
        if [[ "$attempt" == 3 ]] || ! grep -Eq 'Received status code 429|HTTP (status )?429|status code: 429' "$reports/$project-$attempt.log"; then
            return "$rc"
        fi
        sleep "$((attempt * 12))"
    done
}

gradle_args=(--no-daemon --console=plain "-Dmaven.repo.local=$repo" --init-script "$GITHUB_WORKSPACE/agent/ci/local-artifacts.init.gradle")
build candid gradle "${gradle_args[@]}" -p candid -PrunJavacc=true clean publishToMavenLocal
build agent gradle "${gradle_args[@]}" -p agent clean publishToMavenLocal
build management gradle "${gradle_args[@]}" -p management clean publishToMavenLocal
printf '<settings/>\n' > "$work/settings.xml"
maven_args=(-B -ntp -s "$work/settings.xml" "-Dmaven.repo.local=$repo")
build plugin mvn "${maven_args[@]}" -f plugin/pom.xml clean install
(
    cd "$repo"
    for project in candid agent management maven-plugin; do
        sha256sum "org/ic4j/ic4j-$project/0.8.5/ic4j-$project-0.8.5.jar"
    done
) | tee "$reports/dependencies.sha256"
cmp plugin/target/ic4j-maven-plugin-0.8.5.jar "$repo/org/ic4j/ic4j-maven-plugin/0.8.5/ic4j-maven-plugin-0.8.5.jar"
python3 - "$repo/org/ic4j/ic4j-maven-plugin/0.8.5/ic4j-maven-plugin-0.8.5.jar" <<'PY' | tee "$reports/plugin-descriptor.txt"
from struct import unpack
from sys import argv
from xml.etree import ElementTree
from zipfile import ZipFile
with ZipFile(argv[1]) as jar:
    descriptor = ElementTree.fromstring(jar.read('META-INF/maven/plugin.xml'))
    phases = {m.findtext('goal'): m.findtext('phase') for m in descriptor.findall('./mojos/mojo')}
    assert phases == {'install': 'install', 'uninstall': 'clean'}, phases
    classes = [n for n in jar.namelist() if n.endswith('.class')]
    assert classes and all(unpack('>H', jar.read(n)[6:8])[0] == 52 for n in classes)
    print('install and uninstall lifecycle bindings verified; all plugin classes target Java 8 bytecode')
PY

archive="$work/dfx.tar.gz"
curl --fail --silent --show-error --location --retry 3 --max-time 180 \
    https://github.com/dfinity/sdk/releases/download/0.32.0/dfx-0.32.0-x86_64-linux.tar.gz -o "$archive"
printf '%s  %s\n' 218dad11e0519e11c7a310b5c7cb1eadff109bb5db1ea3432f08c870432a80fc "$archive" | sha256sum -c -
mkdir "$work/bin" "$work/fixture"
tar -xzf "$archive" -C "$work/bin" dfx
export PATH="$work/bin:$PATH" DFX_CONFIG_ROOT="$work/dfx-config"
mkdir -m 700 "$DFX_CONFIG_ROOT"
unset DFX_NETWORK DFX_IDENTITY
dfx --version | tee "$reports/dfx-version.txt"
[[ "$(dfx --version)" == 'dfx 0.32.0' ]]
cd "$work/fixture"
cp "$GITHUB_WORKSPACE/plugin/ci/consumer.xml" pom.xml
cp "$GITHUB_WORKSPACE/plugin/usage-example/hello.wasm" fixture.wasm
printf 'service : (text) -> { peek : () -> (text) query };\n' > fixture.did
printf '{"canisters":{"fixture":{"type":"custom","candid":"fixture.did","wasm":"fixture.wasm"}}}\n' > dfx.json
trap 'dfx stop >> "$reports/dfx.log" 2>&1 || true; rm -f "$work/fixture/fixture.pem"; rm -rf "$DFX_CONFIG_ROOT"' EXIT

# A throwaway secp256k1 identity controls only the new fixture. Do not write
# its PEM, DFX identity export or setup output into the uploaded report.
(umask 077; openssl genpkey -algorithm EC -pkeyopt ec_paramgen_curve:secp256k1 -out fixture.pem)
dfx identity import ci-fixture fixture.pem --storage-mode plaintext >/dev/null 2>&1
dfx start --clean --host 127.0.0.1:4943 --background > "$reports/dfx.log" 2>&1
curl --fail --silent --show-error --retry 20 --retry-delay 1 --retry-connrefused --max-time 3 http://127.0.0.1:4943/api/v2/status >/dev/null
dfx canister create fixture --network local --identity ci-fixture --no-wallet
canister="$(dfx canister id fixture --network local)"
[[ "$canister" =~ ^[a-z0-9]+(-[a-z0-9]+)+$ ]]
printf 'Replica fixture %s\n' "$canister" | tee "$reports/fixture.txt"

timeout 180 mvn -o "${maven_args[@]}" -DfixtureCanister="$canister" install 2>&1 | tee "$reports/consumer-install.log"
grep -F 'ic4j:0.8.5:install (deploy)' "$reports/consumer-install.log"
dfx canister call fixture peek --network local --identity ci-fixture --query 2>&1 | tee "$reports/fixture-query.txt"
grep -xF '("from Maven CI")' "$reports/fixture-query.txt"

timeout 180 mvn -o "${maven_args[@]}" -DfixtureCanister="$canister" clean 2>&1 | tee "$reports/consumer-uninstall.log"
grep -F 'ic4j:0.8.5:uninstall (remove-code)' "$reports/consumer-uninstall.log"
dfx canister status fixture --network local --identity ci-fixture 2>&1 | tee "$reports/fixture-after-uninstall.txt"
grep -iEx 'Status: stopped' "$reports/fixture-after-uninstall.txt"
grep -iEx 'Module hash: none' "$reports/fixture-after-uninstall.txt"
(cd "$repo"; sha256sum -c "$reports/dependencies.sha256")
printf 'PASS: external Maven install, ICP query, external Maven clean, and retained empty canister.\n' | tee "$reports/verification.txt"
