package im.tretyakov.dbqueue.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import ru.yandex.money.common.dbqueue.config.DatabaseDialect

@ConfigurationProperties(prefix = "spring.db-queue")
data class DbQueueProperties(
        var enabled: Boolean = false,
        var createSchema: Boolean = false,
        var dialect: DatabaseDialect = DatabaseDialect.POSTGRESQL,
        var config: MutableMap<String, QueueConfigurationProperties> = mutableMapOf()
) {

    fun isEnabled(): Boolean {
        return enabled
    }

    fun isCreateSchema(): Boolean {
        return createSchema
    }
}