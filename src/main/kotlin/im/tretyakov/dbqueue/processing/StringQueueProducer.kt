package im.tretyakov.dbqueue.processing

import ru.yandex.money.common.dbqueue.api.EnqueueParams
import ru.yandex.money.common.dbqueue.api.QueueProducer
import ru.yandex.money.common.dbqueue.api.TaskPayloadTransformer
import ru.yandex.money.common.dbqueue.dao.QueueDao
import ru.yandex.money.common.dbqueue.settings.QueueLocation

class StringQueueProducer(
        private val queueLocation: QueueLocation,
        private val payloadTransformer: TaskPayloadTransformer<String>,
        private val queueDao: QueueDao
): QueueProducer<String> {

    override fun enqueue(enqueueParams: EnqueueParams<String>): Long {
        return queueDao.enqueue(queueLocation, enqueueParams)
    }

    override fun getPayloadTransformer(): TaskPayloadTransformer<String> {
        return payloadTransformer
    }
}