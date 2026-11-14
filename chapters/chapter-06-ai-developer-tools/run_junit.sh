#!/bin/sh
set -eu
cd "$(dirname "$0")"
: "${JUNIT_CLASSPATH:?Set JUNIT_CLASSPATH to the JUnit dependencies; see README}"
result_dir=$(mktemp -d "${TMPDIR:-/tmp}/chapter06-junit.XXXXXX")
printf 'Fresh JUnit result directory: %s\n' "$result_dir"
for variant in corrected without-confirmed-guard; do
    mkdir -p "$result_dir/$variant"
    if [ "$variant" = corrected ]; then order=implementation/Order.java; else order=mutations/without-confirmed-guard/Order.java; fi
    javac --release 21 -cp "$JUNIT_CLASSPATH" -d "$result_dir/$variant" "$order" testing/ConfirmationTests.java testing/LocalJUnitRunner.java
    test_cp="$result_dir/$variant:$JUNIT_CLASSPATH"
    java -cp "$test_cp" LocalJUnitRunner WeakConfirmationTest
    status=0
    java -cp "$test_cp" LocalJUnitRunner StrongConfirmationTest || status=$?
    if [ "$variant" = corrected ]; then test "$status" = 0; else test "$status" = 1; fi
    java -cp "$test_cp" LocalJUnitRunner PrerequisiteTests
done
printf 'Weak/strong comparison matched the expected outcomes.\n'
