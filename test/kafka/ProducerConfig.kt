package kafka

import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import java.util.*

fun producerConfig(bootstrapServers: String): Properties = Properties().apply {
    put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
    put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java)
    put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java)
}
