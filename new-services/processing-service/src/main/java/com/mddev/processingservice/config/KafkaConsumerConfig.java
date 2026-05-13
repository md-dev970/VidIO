package com.mddev.processingservice.config;

import com.mddev.processingservice.event.VideoUploadedEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

@Configuration
public class KafkaConsumerConfig {

    @Bean
    ConsumerFactory<String, VideoUploadedEvent> uploadedEventConsumerFactory(KafkaProperties properties) {
        var consumerProperties = properties.buildConsumerProperties(null);
        consumerProperties.put(ConsumerConfig.GROUP_ID_CONFIG, "processing-service");
        return new DefaultKafkaConsumerFactory<>(
                consumerProperties,
                null,
                new JsonDeserializer<>(VideoUploadedEvent.class, false)
        );
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, VideoUploadedEvent> uploadedEventKafkaListenerContainerFactory(
            ConsumerFactory<String, VideoUploadedEvent> uploadedEventConsumerFactory) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, VideoUploadedEvent>();
        factory.setConsumerFactory(uploadedEventConsumerFactory);
        return factory;
    }
}
