#!/usr/bin/env bash
# Signs the local staging repository created by `./gradlew publish` (build/maven),
# bundles it and uploads it to the Maven Central Publisher Portal.
# https://central.sonatype.org/publish/publish-portal-api/
#
# Usage: scripts/publish-maven-central.sh <staging-repo-dir> [--dry-run]
#
# Environment:
#   SIGNING_KEY                  ASCII armored private GPG key
#   SIGNING_PASSWORD             passphrase of the key (may be empty)
#   MAVEN_CENTRAL_USERNAME       Central Portal user token username
#   MAVEN_CENTRAL_PASSWORD       Central Portal user token password
#   MAVEN_CENTRAL_PUBLISHING_TYPE  AUTOMATIC (default) or USER_MANAGED (publish manually in the portal)
#   MAVEN_CENTRAL_TIMEOUT        seconds to wait for validation/publishing (default 1800)
set -euo pipefail

REPO_DIR="${1:?usage: $0 <staging-repo-dir> [--dry-run]}"
DRY_RUN="${2:-}"
PUBLISHING_TYPE="${MAVEN_CENTRAL_PUBLISHING_TYPE:-AUTOMATIC}"
TIMEOUT="${MAVEN_CENTRAL_TIMEOUT:-1800}"
API="https://central.sonatype.com/api/v1/publisher"

if [[ "$PUBLISHING_TYPE" != "AUTOMATIC" && "$PUBLISHING_TYPE" != "USER_MANAGED" ]]; then
  echo "Invalid MAVEN_CENTRAL_PUBLISHING_TYPE: $PUBLISHING_TYPE" >&2
  exit 1
fi

if [[ ! -d "$REPO_DIR/io" ]]; then
  echo "No staged artifacts found in $REPO_DIR" >&2
  exit 1
fi

WORK_DIR="$(mktemp -d)"
trap 'rm -rf "$WORK_DIR"' EXIT
BUNDLE_DIR="$WORK_DIR/bundle"
cp -r "$REPO_DIR" "$BUNDLE_DIR"

# maven-metadata.xml is maintained by Central itself, it must not be part of the bundle
find "$BUNDLE_DIR" -name 'maven-metadata.xml*' -delete

if find "$BUNDLE_DIR" -name '*-SNAPSHOT*' | grep -q .; then
  echo "SNAPSHOT versions cannot be published to Maven Central" >&2
  exit 1
fi

# Sign every file except checksums, Central does not need signatures for those
export GNUPGHOME="$WORK_DIR/gnupg"
mkdir -m 700 "$GNUPGHOME"
gpg --batch --quiet --import <<< "${SIGNING_KEY:?SIGNING_KEY is not set}"
count=0
while IFS= read -r -d '' file; do
  gpg --batch --yes --quiet --pinentry-mode loopback --passphrase-fd 0 \
    --armor --detach-sign --output "$file.asc" "$file" <<< "${SIGNING_PASSWORD:-}"
  count=$((count + 1))
done < <(find "$BUNDLE_DIR" -type f ! -name '*.md5' ! -name '*.sha1' ! -name '*.sha256' ! -name '*.sha512' -print0)
echo "Signed $count files"

# Every file Central receives needs a .md5 and .sha1 checksum, Gradle already wrote them
missing=0
while IFS= read -r -d '' file; do
  for ext in md5 sha1; do
    if [[ ! -f "$file.$ext" ]]; then
      echo "Missing checksum $file.$ext" >&2
      missing=1
    fi
  done
done < <(find "$BUNDLE_DIR" -type f ! -name '*.md5' ! -name '*.sha1' ! -name '*.sha256' ! -name '*.sha512' ! -name '*.asc' -print0)
[[ "$missing" == 0 ]] || exit 1

BUNDLE="$WORK_DIR/central-bundle.zip"
(cd "$BUNDLE_DIR" && zip -qr "$BUNDLE" .)
echo "Created bundle with $(find "$BUNDLE_DIR" -name '*.pom' | wc -l) components ($(du -h "$BUNDLE" | cut -f1))"

if [[ "$DRY_RUN" == "--dry-run" ]]; then
  cp "$BUNDLE" "${BUNDLE_OUTPUT:-central-bundle.zip}"
  echo "Dry run, not uploading. Bundle written to ${BUNDLE_OUTPUT:-central-bundle.zip}"
  exit 0
fi

AUTH="Authorization: Bearer $(printf '%s:%s' "${MAVEN_CENTRAL_USERNAME:?}" "${MAVEN_CENTRAL_PASSWORD:?}" | base64 | tr -d '\n')"
VERSION="$(find "$BUNDLE_DIR" -name '*.pom' | head -n1 | xargs dirname | xargs basename)"
NAME="headlessmc-$VERSION"

echo "Uploading $NAME with publishingType=$PUBLISHING_TYPE"
DEPLOYMENT_ID="$(curl --silent --show-error --fail-with-body \
  --header "$AUTH" \
  --form "bundle=@$BUNDLE;type=application/octet-stream" \
  "$API/upload?name=$NAME&publishingType=$PUBLISHING_TYPE")"
echo "Deployment id: $DEPLOYMENT_ID"
echo "https://central.sonatype.com/publishing/deployments"

deadline=$((SECONDS + TIMEOUT))
while (( SECONDS < deadline )); do
  status="$(curl --silent --show-error --fail-with-body --request POST \
    --header "$AUTH" "$API/status?id=$DEPLOYMENT_ID")"
  state="$(jq -r '.deploymentState' <<< "$status")"
  echo "$(date -u +%T) $state"
  case "$state" in
    PUBLISHED)
      echo "Published to Maven Central"
      exit 0
      ;;
    PUBLISHING)
      # validation passed, from here on Central finishes the release by itself, which can take a while
      echo "Validation passed, Central is publishing the deployment"
      exit 0
      ;;
    VALIDATED)
      if [[ "$PUBLISHING_TYPE" == "USER_MANAGED" ]]; then
        echo "Validation passed, publish the deployment manually at https://central.sonatype.com/publishing/deployments"
        exit 0
      fi
      ;;
    FAILED)
      echo "Deployment failed:" >&2
      jq '.errors' <<< "$status" >&2
      exit 1
      ;;
  esac
  sleep 15
done

echo "Timed out waiting for deployment $DEPLOYMENT_ID" >&2
exit 1
