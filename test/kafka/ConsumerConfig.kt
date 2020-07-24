package kafka

import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import java.util.*

fun consumerConfig(bootstrapServers: String): Properties = Properties().apply {
    put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
    put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
    put(ConsumerConfig.GROUP_ID_CONFIG, "rekrutteringsbistand-statistikk-api")
    put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java)
    put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java)
}
