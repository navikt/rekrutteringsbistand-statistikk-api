package kafka

import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import java.util.*

fun consumerConfig(bootstrapServers: String, schemaRegistryUrl: String): Properties = Properties().apply {
    put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
    put(KafkaAvroDeserializerConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryUrl)
    put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true)

    put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
    put(ConsumerConfig.GROUP_ID_CONFIG, "rekrutteringsbistand-statistikk-api-v1")
    put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer::class.java)
    put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer::class.java)
}
