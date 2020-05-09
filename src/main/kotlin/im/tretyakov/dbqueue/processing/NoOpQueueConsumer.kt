package im.tretyakov.dbqueue.processing

import ru.yandex.money.common.dbqueue.api.QueueConsumer
import ru.yandex.money.common.dbqueue.api.Task
import ru.yandex.money.common.dbqueue.api.TaskExecutionResult
import ru.yandex.money.common.dbqueue.api.TaskPayloadTransformer
import ru.yandex.money.common.dbqueue.api.impl.NoopPayloadTransformer
import ru.yandex.money.common.dbqueue.settings.QueueConfig

data class NoOpQueueConsumer(private val queueConfig: QueueConfig): QueueConsumer<String> {

    override fun getQueueConfig(): QueueConfig {
        return queueConfig
    }

    override fun getPayloadTransformer(): TaskPayloadTransformer<String> {
        return NoopPayloadTransformer.getInstance()
    }

    override fun execute(task: Task<String>): TaskExecutionResult {
        return TaskExecutionResult.finish()
    }
}