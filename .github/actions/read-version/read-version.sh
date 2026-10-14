#!/usr/bin/env bash
set -euo pipefail

properties_file=${1:-gradle.properties}
version=''
while IFS= read -r line || [[ -n "$line" ]]; do
  line=${line%$'\r'}
  if [[ "$line" =~ ^[[:space:]]*version[[:space:]]*[=:][[:space:]]*(.*)$ ]]; then
    version=${BASH_REMATCH[1]}
  fi
done < "$properties_file"

if [[ -z "$version" || "$version" =~ ^[[:space:]]*$ ]]; then
  echo "No non-empty, uncommented version property found in $properties_file." >&2
  exit 1
fi

snapshot=false
if [[ "$version" == *SNAPSHOT ]]; then
  snapshot=true
fi

echo "Version is $version"
echo "snapshot = $snapshot"
if [[ -n "${GITHUB_OUTPUT:-}" ]]; then
  echo "version=$version" >> "$GITHUB_OUTPUT"
  echo "snapshot=$snapshot" >> "$GITHUB_OUTPUT"
fi
