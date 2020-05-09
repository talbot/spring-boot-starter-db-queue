package im.tretyakov.dbqueue.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import ru.yandex.money.common.dbqueue.settings.ProcessingMode
import ru.yandex.money.common.dbqueue.settings.TaskRetryType
import java.time.Duration

@ConfigurationProperties(prefix = "spring.db-queue.config")
data class QueueConfigurationProperties(
        var location: String = "tasks",
        var shards: MutableList<String> = mutableListOf("main"),
        var threadCount: Int = 1,
        var noTaskTimeout: Duration = Duration.ofSeconds(5L),
        var betweenTaskTimeout: Duration = Duration.ofSeconds(1L),
        var fatalCrashTimeout: Duration = Duration.ofMinutes(1L),
        var retryInterval: Duration = Duration.ofMinutes(1L),
        var retryType: TaskRetryType = TaskRetryType.LINEAR_BACKOFF,
        var processingMode: ProcessingMode = ProcessingMode.SEPARATE_TRANSACTIONS,
        var retry: RetryProperties = RetryProperties(),
        var additionalSettings: MutableMap<String, String> = mutableMapOf(),
        var queueExternalColumns: MutableList<String> = mutableListOf()
)