package im.tretyakov.dbqueue.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import ru.yandex.money.common.dbqueue.settings.ReenqueueRetryType
import java.time.Duration

@ConfigurationProperties(prefix = "spring.db-queue.config.retry")
data class RetryProperties(
        var type: ReenqueueRetryType = ReenqueueRetryType.FIXED,
        var initialDelay: Duration = Duration.ofSeconds(1L),
        var fixedDelay: Duration = Duration.ofSeconds(5L),
        var arithmeticStep: Duration = Duration.ofSeconds(2L),
        var geometricRatio: Long = 2L,
        var sequentialPlan: List<Duration> = listOf(
                Duration.ofSeconds(1L),
                Duration.ofSeconds(2L),
                Duration.ofSeconds(3L),
                Duration.ofSeconds(5L),
                Duration.ofSeconds(8L),
                Duration.ofSeconds(13L),
                Duration.ofSeconds(21L),
                Duration.ofSeconds(34L),
                Duration.ofSeconds(55L),
                Duration.ofSeconds(89L)
        )
)