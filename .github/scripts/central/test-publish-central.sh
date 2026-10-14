#!/usr/bin/env bash
set -euo pipefail
script_dir=$(cd "$(dirname "$0")" && pwd)
temporary=$(mktemp -d)
trap 'rm -rf "$temporary"' EXIT
mkdir -p "$temporary/bin" "$temporary/work"
export PATH="$temporary/bin:$PATH"
export FIXTURE_ROOT="$temporary"
export ORG_GRADLE_PROJECT_repoUsername=fixture
export ORG_GRADLE_PROJECT_repoPassword=fixture
export SIGNING_KEY=fixture
export SIGNING_KEY_ID=fixture
export SIGNING_PASSWORD=fixture
export CENTRAL_POLL_INTERVAL=0 CENTRAL_WAIT_SECONDS=5
export CENTRAL_STATE_FILE="$temporary/state.json"

cat > "$temporary/work/gradlew" <<'SH'
#!/usr/bin/env bash
printf '%s\n' "$*" >> "$FIXTURE_ROOT/gradle-calls"
echo "Created staging repository 'fixture-repo' at fixture"
exit "${UPLOAD_RESULT:-0}"
SH
cat > "$temporary/bin/curl" <<'SH'
#!/usr/bin/env bash
set -eu
output=''
while (( $# )); do
  case "$1" in
    --output) output=$2; shift 2 ;;
    *) url=$1; shift ;;
  esac
done
echo "$url" >> "$FIXTURE_ROOT/http-calls"
body=''
code=200
case "$url" in
  */manual/search/*)
    if [[ -f "$FIXTURE_ROOT/submitted" ]]; then
      body='{"repositories":[{"key":"fixture-repo","portal_deployment_id":"fixture-deployment"}]}'
    else
      body='{"repositories":[{"key":"fixture-repo"}]}'
    fi ;;
  */manual/upload/*)
    [[ "${SCENARIO:-}" == uncertain ]] || touch "$FIXTURE_ROOT/submitted"
    code=524; body='error code: 524' ;;
  */status*)
    if [[ "${SCENARIO:-}" == failed ]]; then
      body='{"deploymentState":"FAILED","errors":{"artifact":["missing signature"]}}'
    elif [[ "${SCENARIO:-}" == auth ]]; then
      code=401
    elif [[ "${SCENARIO:-}" == unknown ]]; then
      body='{"deploymentState":"OTHER"}'
    elif [[ -f "$FIXTURE_ROOT/published" ]]; then
      body='{"deploymentState":"PUBLISHED"}'
    else
      body='{"deploymentState":"VALIDATED"}'
    fi ;;
  */deployment/*) touch "$FIXTURE_ROOT/published"; code=204 ;;
  *) exit 2 ;;
esac
printf '%s' "$body" > "$output"
printf '%s' "$code"
SH
chmod +x "$temporary/bin/curl" "$temporary/work/gradlew"
cd "$temporary/work"

bash "$script_dir/publish-central.sh" --snapshot
[[ ! -f "$CENTRAL_STATE_FILE" && ! -f "$temporary/http-calls" ]]
echo 'SNAPSHOT direct upload: PASS'

bash "$script_dir/publish-central.sh"
jq -e '.published and .upload_complete and .submission_attempted and .publish_attempted' "$CENTRAL_STATE_FILE" >/dev/null
[[ $(grep -c '/manual/upload/' "$temporary/http-calls") == 1 ]]
[[ $(grep -c '/deployment/' "$temporary/http-calls") == 1 ]]
echo 'Actual curl command path, 524 reconciliation and publication: PASS'

before=$(wc -l < "$temporary/gradle-calls")
bash "$script_dir/publish-central.sh" --resume
[[ $(wc -l < "$temporary/gradle-calls") == "$before" ]]
echo 'Resume without reupload: PASS'

if bash "$script_dir/publish-central.sh" --resume --deployment-id another-deployment > "$temporary/result" 2>&1; then
  echo 'Conflicting recovery ID was accepted' >&2; exit 1
fi
echo 'Conflicting recovery ID rejected: PASS'

if bash "$script_dir/publish-central.sh" > "$temporary/result" 2>&1; then
  echo 'Existing state reuploaded' >&2; exit 1
fi
[[ $(wc -l < "$temporary/gradle-calls") == "$before" ]]
echo 'Existing state prevents duplicate upload: PASS'

for scenario in failed auth unknown; do
  rm -f "$CENTRAL_STATE_FILE"
  if SCENARIO=$scenario bash "$script_dir/publish-central.sh" --resume --deployment-id fixture-deployment > "$temporary/result" 2>&1; then
    echo "Unexpected success: $scenario" >&2; exit 1
  fi
  echo "$scenario fails closed: PASS"
done

rm -f "$CENTRAL_STATE_FILE"
if UPLOAD_RESULT=1 bash "$script_dir/publish-central.sh" > "$temporary/result" 2>&1; then
  echo 'Failed upload was accepted' >&2; exit 1
fi
jq -e '.upload_complete == false' "$CENTRAL_STATE_FILE" >/dev/null
if bash "$script_dir/publish-central.sh" --resume > "$temporary/result" 2>&1; then
  echo 'Incomplete upload resumed' >&2; exit 1
fi
echo 'Failed/incomplete upload cannot publish: PASS'

rm -f "$CENTRAL_STATE_FILE" "$temporary/submitted"
before=$(grep -c '/manual/upload/' "$temporary/http-calls")
if SCENARIO=uncertain CENTRAL_POLL_INTERVAL=1 CENTRAL_WAIT_SECONDS=1 \
  bash "$script_dir/publish-central.sh" > "$temporary/result" 2>&1; then
  echo 'Unconfirmed submission was accepted' >&2; exit 1
fi
after=$(grep -c '/manual/upload/' "$temporary/http-calls")
[[ $after == $((before + 1)) ]]
if SCENARIO=uncertain CENTRAL_POLL_INTERVAL=1 CENTRAL_WAIT_SECONDS=1 \
  bash "$script_dir/publish-central.sh" --resume > "$temporary/result" 2>&1; then
  echo 'Unconfirmed submission recovery was accepted' >&2; exit 1
fi
[[ $(grep -c '/manual/upload/' "$temporary/http-calls") == "$after" ]]
echo 'Uncertain submission times out and never resubmits on resume: PASS'
