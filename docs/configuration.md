# Configuration

Besides the commands and their flags and options,
HeadlessMc creates a `config.properties` file in the `%home/.config/headlessmc` folder.
You can list all config settings with the `config` command.
You can use `config set <setting> <value>` to set config values.

```
> config ls
name                                                       value                                      description
hmc.assets.dummy                                           false                                      Whether to write dummy asset files instead of downloading real assets.
hmc.assets.net.cookies                                     true                                       -
hmc.assets.net.delete-failed-files                         true                                       -
hmc.assets.net.retries                                     0                                          -
hmc.assets.net.user-agent                                  HeadlessMc/3.0.0-SNAPSHOT                  -
hmc.assets.parallel.breaker-failure-rate-threshold         50.0                                       -
hmc.assets.parallel.breaker-half-open-calls                4                                          -
hmc.assets.parallel.breaker-minimum-calls                  8                                          -
hmc.assets.parallel.breaker-open-wait-ms                   5000                                       -
hmc.assets.parallel.breaker-sliding-window-size            32                                         -
hmc.assets.parallel.parallelism                            6                                          -
hmc.assets.parallel.rate-limit-permits                     200                                        -
hmc.assets.parallel.rate-limit-refresh-period-seconds      1                                          -
hmc.assets.parallel.rate-limit-timeout-seconds             30                                         -
hmc.assets.parallel.retry-back-off-factor                  2.0                                        -
hmc.assets.parallel.retry-initial-interval-ms              250                                        -
hmc.assets.parallel.retry-jitter                           0.5                                        -
hmc.assets.parallel.retry-max-attempts                     4                                          -
hmc.assets.parallel.retry-max-interval-ms                  2000                                       -
hmc.assets.url                                             https://resources.download.minecraft.net   Base URL of the asset CDN.
hmc.files.game-for-each-version                            true                                       Whether each version gets its own game directory.
hmc.java.download                                          true                                       Whether missing Java versions may be downloaded automatically.
hmc.java.fail-on-parsing-failure                           false                                      Whether parsing a Java installation that fails should abort instead of being skipped.
hmc.java.max-scan-depth                                    6                                          How deep to scan Java installation directories.
hmc.jline.ansi-colors                                      false                                      Allows you to enable colored terminal output, even if the Jline terminal is disabled.
hmc.jline.enabled                                          true                                       If Jline terminal support is enabled. Might need to be disabled on termux or in IDE terminals.
hmc.jline.persistent-completions                           true                                       Always displays completions when the HeadlessMc shell is used.
hmc.launcher.auto-install                                  true                                       -
hmc.launcher.crash-report-watcher                          false                                      -
hmc.launcher.free                                          true                                       -
hmc.libraries.parallel.breaker-failure-rate-threshold      50.0                                       -
hmc.libraries.parallel.breaker-half-open-calls             4                                          -
hmc.libraries.parallel.breaker-minimum-calls               8                                          -
hmc.libraries.parallel.breaker-open-wait-ms                5000                                       -
hmc.libraries.parallel.breaker-sliding-window-size         32                                         -
hmc.libraries.parallel.parallelism                         1                                          -
hmc.libraries.parallel.rate-limit-permits                  200                                        -
hmc.libraries.parallel.rate-limit-refresh-period-seconds   1                                          -
hmc.libraries.parallel.rate-limit-timeout-seconds          30                                         -
hmc.libraries.parallel.retry-back-off-factor               2.0                                        -
hmc.libraries.parallel.retry-initial-interval-ms           250                                        -
hmc.libraries.parallel.retry-jitter                        0.5                                        -
hmc.libraries.parallel.retry-max-attempts                  4                                          -
hmc.libraries.parallel.retry-max-interval-ms               2000                                       -
hmc.log.console-level                                      WARN                                       Minimum level a message needs to be printed to the console.
hmc.log.file                                               true                                       Whether HeadlessMc writes a log file into the logs directory.
hmc.log.file-level                                         DEBUG                                      Minimum level a message needs to end up in the log file. Levels below quarkus.log.min-level are never recorded.
hmc.log.file-name                                          headlessmc.log                             Name of the log file inside the logs directory.
hmc.log.max-backups                                        5                                          How many rotated log files are kept.
hmc.log.max-file-size                                      10M                                        Size the log file may reach before it is rotated, e.g. 10M.
hmc.log.stacktraces                                        true                                       Whether the console prints full stacktraces instead of just the message of an exception and its causes. The log file always contains full stacktraces.
hmc.logging.patch                                          true                                       -
hmc.net.cookies                                            false                                      Whether to keep a cookie store for downloads.
hmc.net.delete-failed-files                                true                                       Whether a partially downloaded file is deleted when the download fails.
hmc.net.retries                                            1                                          Number of retries per download (0 disables retrying).
hmc.net.user-agent                                         HeadlessMc/3.0.0-SNAPSHOT                  User-Agent header sent with requests.
hmc.parallel.breaker-failure-rate-threshold                50.0                                       Failure rate in percent at which the shared circuit breaker opens.
hmc.parallel.breaker-half-open-calls                       4                                          Number of probe calls allowed while the circuit breaker is half-open.
hmc.parallel.breaker-minimum-calls                         8                                          Minimum number of calls required before the failure rate is evaluated.
hmc.parallel.breaker-open-wait-ms                          5000                                       Time in milliseconds the circuit breaker stays open before probing again.
hmc.parallel.breaker-sliding-window-size                   32                                         Number of most recent calls used to calculate the failure rate.
hmc.parallel.parallelism                                   6                                          Maximum number of tasks in progress at once (0 means one worker per task).
hmc.parallel.rate-limit-permits                            200                                        Number of attempts allowed during each rate limit refresh period.
hmc.parallel.rate-limit-refresh-period-seconds             1                                          Length of one rate limit refresh period in seconds.
hmc.parallel.rate-limit-timeout-seconds                    30                                         Maximum time in seconds a worker may wait for a rate limit permit.
hmc.parallel.retry-back-off-factor                         2.0                                        Multiplier applied to the retry delay after each failed attempt.
hmc.parallel.retry-initial-interval-ms                     250                                        Base delay in milliseconds before the first retry.
hmc.parallel.retry-jitter                                  0.5                                        Randomization factor (0.0-1.0) applied to each retry delay.
hmc.parallel.retry-max-attempts                            4                                          Total number of attempts per task, including the initial attempt.
hmc.parallel.retry-max-interval-ms                         2000                                       Upper bound in milliseconds for a single retry delay.
hmc.test.leave                                             true                                       -
hmc.test.no-timeout                                        false                                      -
hmc.test.server                                            false                                      -
hmc.xvfb.check                                             false                                      Whether to check if HeadlessMc is running with XVFB. For CI/CD pipelines.
```

```
> debug
------------------------------------------
HeadlessMc - 3.0.0
------------------------------------------
Files:
 - Data:   /home/me/.local/share/headlessmc
 - Config: /home/me/.config/headlessmc
 - State:  /home/me/.local/state/headlessmc
 - Cache:  /home/me/.cache/headlessmc
 - Mc:     /home/me/.minecraft
------------------------------------------
Memory:
 - Max:   6928.00 MB
 - Total: 80.00 MB
 - Used:  38.05 MB
 - Free:  41.95 MB
------------------------------------------
```
