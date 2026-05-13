package com.mddev.videoservice.config;

import com.mddev.videoservice.event.VideoProcessingCompletedEvent;
import com.mddev.videoservice.event.VideoProcessingFailedEvent;
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
    ConsumerFactory<String, VideoProcessingCompletedEvent> completedEventConsumerFactory(KafkaProperties properties) {
        var consumerProperties = properties.buildConsumerProperties(null);
        consumerProperties.put(ConsumerConfig.GROUP_ID_CONFIG, "video-service");
        return new DefaultKafkaConsumerFactory<>(
                consumerProperties,
                null,
                new JsonDeserializer<>(VideoProcessingCompletedEvent.class, false)
        );
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, VideoProcessingCompletedEvent> completedEventKafkaListenerContainerFactory(
            ConsumerFactory<String, VideoProcessingCompletedEvent> completedEventConsumerFactory) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, VideoProcessingCompletedEvent>();
        factory.setConsumerFactory(completedEventConsumerFactory);
        return factory;
    }

    @Bean
    ConsumerFactory<String, VideoProcessingFailedEvent> failedEventConsumerFactory(KafkaProperties properties) {
        var consumerProperties = properties.buildConsumerProperties(null);
        consumerProperties.put(ConsumerConfig.GROUP_ID_CONFIG, "video-service");
        return new DefaultKafkaConsumerFactory<>(
                consumerProperties,
                null,
                new JsonDeserializer<>(VideoProcessingFailedEvent.class, false)
        );
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, VideoProcessingFailedEvent> failedEventKafkaListenerContainerFactory(
            ConsumerFactory<String, VideoProcessingFailedEvent> failedEventConsumerFactory) {
        var factory = new ConcurrentKafkaListenerContainerFactory<String, VideoProcessingFailedEvent>();
        factory.setConsumerFactory(failedEventConsumerFactory);
        return factory;
    }
}
