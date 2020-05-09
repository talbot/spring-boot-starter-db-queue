package im.tretyakov.dbqueue.config

import im.tretyakov.dbqueue.processing.NoOpQueueConsumer
import im.tretyakov.dbqueue.processing.StringQueueProducer
import im.tretyakov.dbqueue.properties.DbQueueProperties
import im.tretyakov.dbqueue.properties.QueueConfigurationProperties
import im.tretyakov.dbqueue.properties.RetryProperties
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.JdbcOperations
import org.springframework.transaction.support.TransactionOperations
import ru.yandex.money.common.dbqueue.api.QueueConsumer
import ru.yandex.money.common.dbqueue.api.QueueProducer
import ru.yandex.money.common.dbqueue.api.TaskPayloadTransformer
import ru.yandex.money.common.dbqueue.api.impl.NoopPayloadTransformer
import ru.yandex.money.common.dbqueue.config.QueueService
import ru.yandex.money.common.dbqueue.config.QueueShard
import ru.yandex.money.common.dbqueue.config.QueueShardId
import ru.yandex.money.common.dbqueue.config.QueueTableSchema
import ru.yandex.money.common.dbqueue.config.TaskLifecycleListener
import ru.yandex.money.common.dbqueue.config.ThreadLifecycleListener
import ru.yandex.money.common.dbqueue.config.impl.NoopTaskLifecycleListener
import ru.yandex.money.common.dbqueue.config.impl.NoopThreadLifecycleListener
import ru.yandex.money.common.dbqueue.dao.MssqlQueueDao
import ru.yandex.money.common.dbqueue.dao.PostgresQueueDao
import ru.yandex.money.common.dbqueue.dao.QueueDao
import ru.yandex.money.common.dbqueue.settings.QueueConfig
import ru.yandex.money.common.dbqueue.settings.QueueId
import ru.yandex.money.common.dbqueue.settings.QueueLocation
import ru.yandex.money.common.dbqueue.settings.QueueSettings
import ru.yandex.money.common.dbqueue.settings.ReenqueueRetrySettings

@Configuration
@EnableConfigurationProperties(
        DbQueueProperties::class,
        QueueConfigurationProperties::class,
        RetryProperties::class
)
@ConditionalOnProperty(name = ["spring.db-queue.enabled"], havingValue = "true", matchIfMissing = false)
class DbQueueAutoConfiguration {

    @Autowired
    private lateinit var dbQueueProperties: DbQueueProperties

    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    @Bean
    @ConditionalOnBean(JdbcOperations::class, TransactionOperations::class)
    fun queueShards(jdbcTemplate: JdbcOperations, transactionTemplate: TransactionOperations): List<QueueShard> {
        val config = dbQueueProperties.config.entries.single { true }.value
        return config.shards.map {
            QueueShard(
                    dbQueueProperties.dialect,
                    QueueTableSchema.builder().build(),
                    QueueShardId(it),
                    jdbcTemplate,
                    transactionTemplate
            )
        }
    }

    @Bean
    @ConditionalOnMissingBean(QueueConfig::class)
    fun queueConfig(): QueueConfig {
        val queueConfiguration = dbQueueProperties.config.entries.single { true }
        val config = queueConfiguration.value
        return QueueConfig(
                QueueLocation.builder()
                        .withQueueId(QueueId(queueConfiguration.key))
                        .withTableName(config.location)
                        .build(),
                QueueSettings.builder()
                        .withThreadCount(config.threadCount)
                        .withProcessingMode(config.processingMode)
                        .withNoTaskTimeout(config.noTaskTimeout)
                        .withBetweenTaskTimeout(config.betweenTaskTimeout)
                        .withFatalCrashTimeout(config.fatalCrashTimeout)
                        .withRetryType(config.retryType)
                        .withRetryInterval(config.retryInterval)
                        .withReenqueueRetrySettings(ReenqueueRetrySettings
                                .builder(config.retry.type)
                                .withInitialDelay(config.retry.initialDelay)
                                .withFixedDelay(config.retry.fixedDelay)
                                .withArithmeticStep(config.retry.arithmeticStep)
                                .withGeometricRatio(config.retry.geometricRatio)
                                .withSequentialPlan(config.retry.sequentialPlan)
                                .build()
                        )
                        .build()
        )
    }

    @Bean
    @ConditionalOnMissingBean(QueueDao::class)
    @ConditionalOnBean(JdbcOperations::class)
    @ConditionalOnProperty(name = ["spring.db-queue.dialect"], havingValue = "POSTGRESQL", matchIfMissing = false)
    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    fun postgresQueueDao(jdbcTemplate: JdbcOperations): PostgresQueueDao {
        val config = dbQueueProperties.config.entries.single { true }.value
        return PostgresQueueDao(jdbcTemplate, QueueTableSchema.builder()
                .withExtFields(config.queueExternalColumns)
                .build()
        )
    }

    @Bean
    @ConditionalOnMissingBean(QueueDao::class)
    @ConditionalOnBean(JdbcOperations::class)
    @ConditionalOnProperty(name = ["spring.db-queue.dialect"], havingValue = "MSSQL", matchIfMissing = false)
    @Suppress("SpringJavaInjectionPointsAutowiringInspection")
    fun mssqlQueueDao(jdbcTemplate: JdbcOperations): MssqlQueueDao {
        val config = dbQueueProperties.config.entries.single { true }.value
        return MssqlQueueDao(jdbcTemplate, QueueTableSchema.builder()
                .withExtFields(config.queueExternalColumns)
                .build()
        )
    }

    @Bean
    @ConditionalOnMissingBean(QueueService::class)
    fun queueService(
            shards: List<QueueShard>,
            consumer: QueueConsumer<out Any>,
            threadLifecycleListener: ThreadLifecycleListener,
            taskLifecycleListener: TaskLifecycleListener
    ): QueueService {
        val service = QueueService(shards, threadLifecycleListener, taskLifecycleListener)
        service.registerQueue(consumer)
        service.start()
        return service
    }

    @Bean
    @ConditionalOnMissingBean(TaskPayloadTransformer::class)
    fun noOpTaskPayloadTransformer(): TaskPayloadTransformer<String> {
        return NoopPayloadTransformer.getInstance()
    }

    @Bean
    @ConditionalOnMissingBean(QueueConsumer::class)
    fun noOpQueueConsumer(queueConfig: QueueConfig): QueueConsumer<String> {
        return NoOpQueueConsumer(queueConfig)
    }

    @Bean
    @ConditionalOnProperty(name = ["spring.db-queue.dialect"], havingValue = "POSTGRESQL", matchIfMissing = false)
    @ConditionalOnBean(PostgresQueueDao::class)
    @ConditionalOnMissingBean(QueueProducer::class)
    fun postgresStringQueueProducer(
            queueConfig: QueueConfig,
            taskPayloadTransformer: TaskPayloadTransformer<String>,
            queueDao: PostgresQueueDao
    ): QueueProducer<String> {
        return StringQueueProducer(queueConfig.location, taskPayloadTransformer, queueDao)
    }

    @Bean
    @ConditionalOnProperty(name = ["spring.db-queue.dialect"], havingValue = "MSSQL", matchIfMissing = false)
    @ConditionalOnBean(MssqlQueueDao::class)
    @ConditionalOnMissingBean(QueueProducer::class)
    fun mssqlStringQueueProducer(
            queueConfig: QueueConfig,
            taskPayloadTransformer: TaskPayloadTransformer<String>,
            queueDao: MssqlQueueDao
    ): QueueProducer<String> {
        return StringQueueProducer(queueConfig.location, taskPayloadTransformer, queueDao)
    }

    @Bean
    @ConditionalOnMissingBean(ThreadLifecycleListener::class)
    fun noOpThreadLifecycleListener(): ThreadLifecycleListener {
        return NoopThreadLifecycleListener()
    }

    @Bean
    @ConditionalOnMissingBean(TaskLifecycleListener::class)
    fun noOpTaskLifecycleListener(): TaskLifecycleListener {
        return NoopTaskLifecycleListener()
    }
}