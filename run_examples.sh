#!/bin/sh
set -eu
cd "$(dirname "$0")"
repo_dir=$(pwd)
gson_jar=${GSON_JAR:-"$repo_dir/.deps/gson-2.13.2.jar"}
if [ ! -f "$gson_jar" ]; then
    printf 'Gson not found. Follow README.md to download Gson 2.13.2, or set GSON_JAR to its absolute path.\n' >&2
    exit 1
fi
case "$gson_jar" in
    /*) ;;
    *) gson_jar="$repo_dir/$gson_jar" ;;
esac
build_dir=$(mktemp -d "${TMPDIR:-/tmp}/genai-examples.XXXXXX")
trap 'rm -rf "$build_dir"' EXIT
trap 'exit 130' INT
trap 'exit 143' TERM
for chapter in 02 03 04 09 10 11 12 13 14 15; do
    classes="$build_dir/chapter-$chapter"
    mkdir -p "$classes"
    printf '\nChapter %s\n' "$chapter"
    javac --release 21 -cp "$gson_jar" -d "$classes" "chapters/chapter-$chapter/"*.java
    case "$chapter" in
        02) main=RoutingExample ;;
        03) main=CatalogDraft ;;
        04) main=InvoiceProposal ;;
        09) main=JobLedger ;;
        10) main=LoanExtensionWorkflow ;;
        11) main=CatalogHandoff ;;
        12) main=ReleaseGate ;;
        13) main=TrainingSplitAudit ;;
        14) main=ModelRoutePolicy ;;
        15) main=SkillGenieLab ;;
    esac
    if [ "$chapter" = 15 ]; then
        java -cp "$classes:$gson_jar" "$main" --check
        java -cp "$classes:$gson_jar" "$main" --demo
    else
        java -cp "$classes:$gson_jar" "$main"
    fi
    if [ "$chapter" = 02 ]; then
        printf 'GatewayComparison compiled only; no live model calls were made.\n'
    fi
done
printf '\nChapter 06 core experiments\n'
sh chapters/chapter-06-ai-developer-tools/run_all.sh
printf '\nAll core examples matched their expected outcomes. Optional JUnit and live gateway runs are separate.\n'
