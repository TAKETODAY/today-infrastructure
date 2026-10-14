#!/usr/bin/env bash
# Upload once; reconcile uncertain writes before continuing Central publication.
set -euo pipefail
umask 077

state_file=${CENTRAL_STATE_FILE:-build/central-publish-state.json}
staging_url=${CENTRAL_STAGING_URL:-https://ossrh-staging-api.central.sonatype.com}
portal_url=${CENTRAL_PORTAL_URL:-https://central.sonatype.com/api/v1/publisher}
interval=${CENTRAL_POLL_INTERVAL:-30}
wait_seconds=${CENTRAL_WAIT_SECONDS:-14400}
resume=false
repository_key=''
deployment_id=''
upload_complete=false
submission_attempted=false
publish_attempted=false
published=false
snapshot=false

fail() { echo "$*" >&2; exit 1; }
while (( $# )); do
  case "$1" in
    --resume) resume=true; shift ;;
    --snapshot) snapshot=true; shift ;;
    --repository-key|--deployment-id)
      (( $# >= 2 )) || fail "Missing value for $1"
      [[ "$1" == --repository-key ]] && repository_key=$2 || deployment_id=$2
      shift 2 ;;
    *) fail "Unknown argument: $1" ;;
  esac
done

if $snapshot; then
  $resume && fail 'SNAPSHOT does not use deployment recovery'
  [[ -z "$repository_key$deployment_id" ]] || fail 'SNAPSHOT does not accept staging or deployment IDs'
  exec ./gradlew --no-daemon --no-parallel publishMavenPublicationToSonatypeRepository
fi

command -v jq >/dev/null || fail 'jq is required'
[[ "$interval" =~ ^[0-9]+$ && "$wait_seconds" =~ ^[1-9][0-9]*$ ]] || fail 'Invalid polling configuration'
[[ -n "${ORG_GRADLE_PROJECT_repoUsername:-}" && -n "${ORG_GRADLE_PROJECT_repoPassword:-}" ]] || fail 'Central Portal credentials are required'
[[ -n "${SIGNING_KEY:-}" && -n "${SIGNING_KEY_ID:-}" && -n "${SIGNING_PASSWORD:-}" ]] || fail 'Signing credentials are required'
[[ -z "$repository_key$deployment_id" || "$resume" == true ]] || fail 'Explicit IDs require --resume'
mkdir -p "$(dirname "$state_file")"
temporary=$(mktemp -d)
trap 'rm -rf "$temporary"' EXIT
printf 'header = "Authorization: Bearer %s"\n' \
  "$(printf '%s:%s' "$ORG_GRADLE_PROJECT_repoUsername" "$ORG_GRADLE_PROJECT_repoPassword" | base64 | tr -d '\r\n')" > "$temporary/curl.conf"

save_state() {
  jq -n --arg repository_key "$repository_key" --arg deployment_id "$deployment_id" \
    --argjson upload_complete "$upload_complete" --argjson submission_attempted "$submission_attempted" \
    --argjson publish_attempted "$publish_attempted" --argjson published "$published" \
    '{repository_key:$repository_key,deployment_id:$deployment_id,upload_complete:$upload_complete,
      submission_attempted:$submission_attempted,publish_attempted:$publish_attempted,published:$published}' > "$state_file.tmp"
  mv "$state_file.tmp" "$state_file"
}

if [[ -f "$state_file" ]]; then
  $resume || fail "Existing state found; use --resume: $state_file"
  jq -e 'type == "object" and (.repository_key|type == "string")
    and (.deployment_id|type == "string") and (.upload_complete|type == "boolean")
    and (.submission_attempted|type == "boolean") and (.publish_attempted|type == "boolean")
    and (.published|type == "boolean")' "$state_file" >/dev/null || fail 'Invalid state file'
  if [[ -n "$repository_key" && "$repository_key" != "$(jq -r '.repository_key' "$state_file")" ]]; then
    fail 'Repository ID conflicts with saved state; use a separate CENTRAL_STATE_FILE'
  fi
  if [[ -n "$deployment_id" && "$deployment_id" != "$(jq -r '.deployment_id' "$state_file")" ]]; then
    fail 'Deployment ID conflicts with saved state; use a separate CENTRAL_STATE_FILE'
  fi
  repository_key=${repository_key:-$(jq -r '.repository_key // ""' "$state_file")}
  deployment_id=${deployment_id:-$(jq -r '.deployment_id // ""' "$state_file")}
  upload_complete=$(jq -r '.upload_complete' "$state_file")
  submission_attempted=$(jq -r '.submission_attempted' "$state_file")
  publish_attempted=$(jq -r '.publish_attempted' "$state_file")
  published=$(jq -r '.published' "$state_file")
elif $resume; then
  [[ -n "$repository_key$deployment_id" ]] || fail 'Recovery requires a state file or explicit ID'
  # Explicit repository ID asserts that the operator confirmed upload completion.
  upload_complete=true
  save_state
fi

request() {
  local method=$1 url=$2
  http_code=''
  transport_code=0
  http_code=$(curl --config "$temporary/curl.conf" --silent --show-error \
    --connect-timeout 10 --max-time 120 --request "$method" \
    --output "$temporary/response" --write-out '%{http_code}' "$url") || transport_code=$?
}
success() { [[ "$transport_code" == 0 && "$http_code" =~ ^2[0-9][0-9]$ ]]; }
deadline=$((SECONDS + wait_seconds))
pause() {
  (( SECONDS < deadline )) || fail "Central wait timed out; recover using $state_file (repository=$repository_key deployment=$deployment_id)"
  sleep "$interval"
}
observe() {
  while true; do
    request "$1" "$2"
    success && return
    if [[ "$transport_code" == 0 && ! "$http_code" =~ ^5 && "$http_code" != 429 ]]; then
      fail "Central observation failed: HTTP $http_code"
    fi
    echo "Central observation unavailable (HTTP $http_code, curl $transport_code); waiting"
    pause
  done
}
check_write() {
  success && return
  if [[ "$transport_code" == 0 && ! "$http_code" =~ ^5 && "$http_code" != 429 ]]; then
    fail "Central write rejected: HTTP $http_code; check deployment before retrying"
  fi
  echo "Central write outcome uncertain (HTTP $http_code, curl $transport_code); checking status without resubmitting"
}
encode() { jq -rn --arg value "$1" '$value|@uri'; }
find_deployment() {
  observe GET "$staging_url/manual/search/repositories?ip=any&profile_id=cn.taketoday"
  jq -e --arg key "$repository_key" '[.repositories[]|select(.key==$key)]|length==1' "$temporary/response" >/dev/null || fail "Staging repository not uniquely found: $repository_key"
  deployment_id=$(jq -er --arg key "$repository_key" '.repositories[]|select(.key==$key)|(.portal_deployment_id // "")|select(type=="string")' "$temporary/response") || fail 'Invalid deployment ID in repository response'
  [[ -z "$deployment_id" ]] || save_state
}

if ! $resume; then
  # Keep the Gradle result even when tee succeeds; capture the ID on upload failure too.
  upload_result=0
  ./gradlew --console=plain --no-daemon --no-parallel publishMavenPublicationToSonatypeRepository \
    -Psigning.key="$SIGNING_KEY" -Psigning.keyId="$SIGNING_KEY_ID" -Psigning.password="$SIGNING_PASSWORD" \
    2>&1 | tee "$temporary/upload.log" || upload_result=$?
  repository_key=$(sed -n "s/.*Created staging repository '\([^']*\)'.*/\1/p" "$temporary/upload.log")
  [[ -n "$repository_key" && "$repository_key" != *$'\n'* ]] || fail 'Could not capture a unique staging repository ID; inspect Gradle logs'
  save_state
  (( upload_result == 0 )) || fail "Artifact upload failed; incomplete repository recorded in $state_file"
  upload_complete=true
  save_state
fi

deadline=$((SECONDS + wait_seconds))
if [[ -z "$deployment_id" ]]; then
  $upload_complete || fail 'Artifact upload completion is not confirmed'
  [[ -n "$repository_key" ]] || fail 'Missing staging repository ID'
  find_deployment
  if [[ -z "$deployment_id" ]] && ! $submission_attempted; then
    submission_attempted=true
    save_state
    request POST "$staging_url/manual/upload/repository/$(encode "$repository_key")?publishing_type=portal_api"
    check_write
  fi
  while [[ -z "$deployment_id" ]]; do
    pause
    find_deployment
  done
fi

previous=''
while true; do
  observe POST "$portal_url/status?id=$(encode "$deployment_id")"
  status=$(jq -er '.deploymentState|select(type=="string")' "$temporary/response") || fail 'Invalid Portal status response'
  if [[ "$status" != "$previous" ]]; then
    echo "Central deployment $deployment_id: $status"
    previous=$status
  fi
  case "$status" in
    PUBLISHED) published=true; save_state; exit 0 ;;
    FAILED) jq '{deploymentId,deploymentState,errors}' "$temporary/response" >&2; fail 'Central validation failed' ;;
    VALIDATED)
      if ! $publish_attempted; then
        publish_attempted=true
        save_state
        request POST "$portal_url/deployment/$(encode "$deployment_id")"
        check_write
      fi ;;
    PENDING|VALIDATING|PUBLISHING) ;;
    *) fail "Unknown deployment state: $status" ;;
  esac
  pause
done
