package com.turkcell.embeddedservers.configuration;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.EmbeddedKafkaKraftBroker;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Configuration
public class EmbeddedKafkaConfig {
    private static final String[] TOPICS = {"orders","payments"};

    @Bean(destroyMethod = "destroy")
    public EmbeddedKafkaKraftBroker embeddedKafkaBroker(ConfigurableEnvironment env) {
        Locale.setDefault(Locale.US);
        System.setProperty("user.language", "en");
        System.setProperty("user.country", "US");

        Map<String, String> props = new HashMap<>();

        // Client ve controller için ayrı listener’lar + sabit portlar
        props.put("listeners",
                "EXTERNAL://127.0.0.1:29023,CONTROLLER://127.0.0.1:29024");
        props.put("advertised.listeners", "EXTERNAL://127.0.0.1:29023");

        // Listener → security protocol eşlemesi (kritik)
        props.put("listener.security.protocol.map",
                "EXTERNAL:PLAINTEXT,CONTROLLER:PLAINTEXT");

        // Hangi listener broker içi trafik için kullanılacak?
        props.put("inter.broker.listener.name", "EXTERNAL");

        // Controller hangi listener’ı dinliyor?
        props.put("controller.listener.names", "CONTROLLER");

        EmbeddedKafkaKraftBroker broker =
                (EmbeddedKafkaKraftBroker) new EmbeddedKafkaKraftBroker(1, 1,  "payments")
                        // .kafkaPorts(29023) // İsteğe bağlı: artık gerekmez; portu listeners ile sabitledik
                        .brokerProperties(props);

        broker.afterPropertiesSet();

        String brokers = "127.0.0.1:29023"; // client’ların göreceği adres
        System.setProperty("spring.embedded.kafka.brokers", brokers);
        env.getSystemProperties().put("spring.cloud.stream.kafka.binder.brokers", brokers);

        System.out.println("Embedded Kafka Broker started " + brokers);
        return broker;
    }

}
