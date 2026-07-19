#!/usr/bin/env bash
set -euo pipefail

root_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
build_file="${1:-${root_dir}/build.gradle.kts}"
lock_file="${2:-${root_dir}/scripts/ci/cursemaven-lock.tsv}"
repository_dir="${MMCEGE_CURSEMAVEN_MIRROR:-${root_dir}/.gradle/cursemaven-repo}"

if [[ ! -f "${build_file}" ]]; then
    echo "Build file not found: ${build_file}" >&2
    exit 1
fi
if [[ ! -f "${lock_file}" ]]; then
    echo "CurseMaven lock file not found: ${lock_file}" >&2
    exit 1
fi

mkdir -p "${repository_dir}/curse/maven"

mapfile -t coordinates < <(
    grep -oE 'curse\.maven:[A-Za-z0-9._-]+:[0-9]+' "${build_file}" |
        sort -u
)
locked_coordinates="$(
    awk -F '\t' 'NF >= 3 && $1 !~ /^#/ { print $1 }' "${lock_file}" |
        sort -u
)"
build_coordinates="$(printf '%s\n' "${coordinates[@]}")"

if [[ -z "${build_coordinates}" ]]; then
    echo "No CurseMaven coordinates found in ${build_file}" >&2
    exit 1
fi
if [[ "${build_coordinates}" != "${locked_coordinates}" ]]; then
    echo "CurseMaven coordinates do not match ${lock_file}" >&2
    diff -u \
        <(printf '%s\n' "${locked_coordinates}") \
        <(printf '%s\n' "${build_coordinates}") || true
    exit 1
fi

validate_jar() {
    local jar_path="$1"
    local expected_sha256="$2"
    local actual_sha256

    [[ -f "${jar_path}" ]] || return 1
    jar tf "${jar_path}" >/dev/null 2>&1 || return 1
    actual_sha256="$(sha256sum "${jar_path}" | awk '{ print $1 }')"
    [[ "${actual_sha256}" == "${expected_sha256}" ]]
}

download_jar() {
    local artifact="$1"
    local file_id="$2"
    local cdn_path="$3"
    local expected_sha256="$4"
    local jar_path="$5"
    local part_path="${jar_path}.part"
    local candidate
    local -a candidates=(
        "https://mediafiles.forgecdn.net/${cdn_path}"
        "https://mediafilez.forgecdn.net/${cdn_path}"
    )

    rm -f "${part_path}"
    for candidate in "${candidates[@]}"; do
        echo "Downloading ${artifact}:${file_id} from ${candidate}"
        if curl --fail --location --silent --show-error \
            --retry 4 --retry-all-errors --retry-delay 2 \
            --connect-timeout 20 --max-time 600 \
            --output "${part_path}" "${candidate}" &&
            validate_jar "${part_path}" "${expected_sha256}"; then
            mv "${part_path}" "${jar_path}"
            return 0
        fi
        rm -f "${part_path}"
    done

    echo "Failed to download a valid locked JAR for ${artifact}:${file_id}" >&2
    return 1
}

for coordinate in "${coordinates[@]}"; do
    lock_line="$(
        awk -F '\t' -v coordinate="${coordinate}" \
            '$1 == coordinate { print $0; exit }' "${lock_file}"
    )"
    IFS=$'\t' read -r locked_coordinate cdn_path expected_sha256 <<<"${lock_line}"

    artifact="${coordinate#curse.maven:}"
    artifact="${artifact%:*}"
    file_id="${coordinate##*:}"
    artifact_dir="${repository_dir}/curse/maven/${artifact}/${file_id}"
    jar_path="${artifact_dir}/${artifact}-${file_id}.jar"
    pom_path="${artifact_dir}/${artifact}-${file_id}.pom"

    mkdir -p "${artifact_dir}"

    if validate_jar "${jar_path}" "${expected_sha256}"; then
        echo "Using cached ${artifact}:${file_id}"
    else
        rm -f "${jar_path}"
        download_jar \
            "${artifact}" "${file_id}" "${cdn_path}" "${expected_sha256}" "${jar_path}"
    fi

    cat >"${pom_path}" <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>curse.maven</groupId>
    <artifactId>${artifact}</artifactId>
    <version>${file_id}</version>
</project>
EOF
done

echo "Prepared ${#coordinates[@]} locked CurseMaven artifacts in ${repository_dir}"
