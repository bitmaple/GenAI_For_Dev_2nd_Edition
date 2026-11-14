#!/bin/sh
set -eu
cd "$(dirname "$0")"
result_dir=$(mktemp -d "${TMPDIR:-/tmp}/chapter06-checks.XXXXXX")
printf 'Fresh result directory: %s\n' "$result_dir"
check_run() {
    label=$1
    expected=$2
    shift 2
    status=0
    "$@" >"$result_dir/$label.log" 2>&1 || status=$?
    if [ "$status" -ne "$expected" ]; then
        cat "$result_dir/$label.log"
        printf 'Unexpected exit for %s: %s (expected %s)\n' "$label" "$status" "$expected"
        exit 1
    fi
    printf '%s: expected exit %s observed\n' "$label" "$expected"
}
for variant in investigation implementation; do
    mkdir -p "$result_dir/$variant"
    javac --release 21 -d "$result_dir/$variant" "$variant/Order.java" "$variant/OrderChecks.java" RegressionExcerpt.java
done
check_run investigation-baseline 0 java -cp "$result_dir/investigation" OrderChecks
check_run investigation-duplicate 1 java -cp "$result_dir/investigation" OrderChecks duplicate
check_run implementation-baseline 0 java -cp "$result_dir/implementation" OrderChecks
check_run implementation-duplicate 0 java -cp "$result_dir/implementation" OrderChecks duplicate
check_run regression-before 1 java -cp "$result_dir/investigation" RegressionExcerpt
check_run regression-after 0 java -cp "$result_dir/implementation" RegressionExcerpt
grep -q '42 baseline checks passed' "$result_dir/implementation-baseline.log"
grep -q '3 duplicate checks passed' "$result_dir/implementation-duplicate.log"
for variant in without-confirmed-guard without-cancellation-guard; do
    mkdir -p "$result_dir/$variant"
    javac --release 21 -d "$result_dir/$variant" "mutations/$variant/Order.java" implementation/OrderChecks.java
    check_run "$variant" 1 java -cp "$result_dir/$variant" OrderChecks
done
mkdir -p "$result_dir/debugging"
javac --release 21 -d "$result_dir/debugging" debugging/DebuggingChecks.java
for iteration in 1 2 3 4 5; do
    check_run "debugging-$iteration" 0 java -cp "$result_dir/debugging" DebuggingChecks
    cmp "$result_dir/debugging-1.log" "$result_dir/debugging-$iteration.log"
done
cat "$result_dir/debugging-1.log"
for variant in original refactored; do
    mkdir -p "$result_dir/$variant"
    if [ "$variant" = original ]; then order=implementation/Order.java; else order=refactoring/Order.java; fi
    javac --release 21 -d "$result_dir/$variant" "$order" refactoring/ReservationAdapter.java refactoring/RefactoringChecks.java
    check_run "refactoring-$variant" 0 java -cp "$result_dir/$variant" RefactoringChecks
    java -cp "$result_dir/$variant" RefactoringChecks traces >"$result_dir/$variant-traces.txt"
    test "$(wc -l <"$result_dir/$variant-traces.txt" | tr -d ' ')" = 1024
done
cmp "$result_dir/original-traces.txt" "$result_dir/refactored-traces.txt"
printf 'All 1,024 five-operation traces match.\n'
mkdir -p "$result_dir/either-fact"
javac --release 21 -d "$result_dir/either-fact" mutations/either-fact/Order.java refactoring/ReservationAdapter.java refactoring/RefactoringChecks.java
check_run either-fact 1 java -cp "$result_dir/either-fact" RefactoringChecks
mkdir -p "$result_dir/review"
javac --release 21 -d "$result_dir/review" implementation/Order.java review/ReviewChecks.java
check_run caller-observe 0 java -cp "$result_dir/review" ReviewChecks observe
cat "$result_dir/caller-observe.log"
check_run caller-bad-repeat 1 java -cp "$result_dir/review" ReviewChecks proposed-repeat
check_run caller-bad-cancel 1 java -cp "$result_dir/review" ReviewChecks proposed-cancel
check_run caller-corrected 0 java -cp "$result_dir/review" ReviewChecks corrected
printf 'All core experiments matched expected outcomes. JUnit is a separate optional run.\n'
