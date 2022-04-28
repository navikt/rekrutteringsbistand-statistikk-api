package no.nav.statistikkapi.kafka

import io.confluent.kafka.schemaregistry.CompatibilityLevel
import io.confluent.kafka.schemaregistry.rest.SchemaRegistryConfig
import io.confluent.kafka.serializers.KafkaAvroSerializer
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig
import no.nav.statistikkapi.Cluster
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SaslConfigs
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.serialization.StringSerializer
import java.io.File
import java.util.*

class KafkaConfig {

    companion object {
        fun producerConfig(): Properties = Properties().apply {
            put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
            put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT")
            put(SaslConfigs.SASL_MECHANISM, "PLAIN")
            put(SaslConfigs.SASL_JAAS_CONFIG, saslJaasConfig)

            System.getenv("NAV_TRUSTSTORE_PATH")?.let {
                put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL")
                put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, File(it).absolutePath)
                put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, System.getenv("NAV_TRUSTSTORE_PASSWORD"))
            }

            put(KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG, "http://kafka-schema-registry.tpa.svc.nais.local:8081")

            put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java)
            put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer::class.java)
            put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 1000)
        }

        // Servernavn hentet fra: https://confluence.adeo.no/pages/viewpage.action?pageId=239339073
        private val bootstrapServers = when (Cluster.current) {
            Cluster.DEV_FSS -> "b27apvl00045.preprod.local:8443, b27apvl00046.preprod.local:8443, b27apvl00047.preprod.local:8443"
            Cluster.PROD_FSS -> "a01apvl00145.adeo.no:8443, a01apvl00146.adeo.no:8443, a01apvl00147.adeo.no:8443, a01apvl00148.adeo.no:8443, a01apvl00149.adeo.no:8443, a01apvl00150.adeo.no:8443"
            Cluster.LOKAL -> throw UnsupportedOperationException()
        }
        private val serviceuserUsername: String = System.getenv("SERVICEUSER_USERNAME")
        private val serviceuserPassword: String = System.getenv("SERVICEUSER_PASSWORD")
        private val saslJaasConfig = "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"${serviceuserUsername}\" password=\"${serviceuserPassword}\";"
    }
}
