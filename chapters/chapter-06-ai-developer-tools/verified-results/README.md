# Recorded local checks

These results were rerun on November 14, 2026, from the publication copy. The environment was macOS on Apple silicon with Eclipse Temurin 25.0.4.1; sources were compiled with `--release 21`. The JUnit comparison used Jupiter 5.11.4 and Platform 1.11.4 from an existing dependency cache. No model account, cloud service or live order system was used.

- [Core experiments](run_all.sh.txt): the script verified the expected passes and deliberately failing variants, including five fresh JVM concurrency runs and 1,024 matching refactoring traces.
- [JUnit comparison](run_junit.sh.txt): the weak test passed both versions, while the stronger test rejected the version without the confirmation guard. The script returned success only when that expected pattern held.

Temporary filesystem paths are replaced with `<temporary-directory>` so the logs do not expose a local account or folder. Test outcomes are unchanged. These are recorded local observations; fresh CI results are available from the repository's Actions tab. The experiments do not establish real-database, broker or provider guarantees.
