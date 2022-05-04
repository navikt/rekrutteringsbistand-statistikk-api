package no.nav.statistikkapi.kafka

import io.confluent.kafka.serializers.KafkaAvroSerializer
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.security.auth.SecurityProtocol
import org.apache.kafka.common.serialization.StringSerializer
import java.util.*

class KafkaConfig {

    companion object {
        fun producerConfig(): Properties = Properties().apply {
            put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, systemEnv("KAFKA_BROKERS"))
            put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, SecurityProtocol.SSL.name)

            put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, "jks")
            put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, "PKCS12")
            put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, systemEnv("KAFKA_TRUSTSTORE_PATH"))
            put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, systemEnv("KAFKA_CREDSTORE_PASSWORD"))
            put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, systemEnv("KAFKA_KEYSTORE_PATH"))
            put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, systemEnv("KAFKA_CREDSTORE_PASSWORD"))

            val schemaregisterUser = systemEnv("KAFKA_SCHEMA_REGISTRY_USER")
            val schemaregisterPassword = systemEnv("KAFKA_SCHEMA_REGISTRY_PASSWORD")
            put(KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG, systemEnv("KAFKA_SCHEMA_REGISTRY"))
            put(KafkaAvroSerializerConfig.USER_INFO_CONFIG, "$schemaregisterUser:$schemaregisterPassword")

            put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java)
            put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer::class.java)
            put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 1000)
        }

    }
}

private fun systemEnv(navn: String) = System.getenv(navn) ?: throw Exception("Mangler $navn som systemvariabel")