package com.mddev.videoservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    NewTopic videoUploadedTopic(@Value("${kafka.topics.video-uploaded}") String topic) {
        return TopicBuilder.name(topic).partitions(1).replicas(1).build();
    }

    @Bean
    NewTopic processingCompletedTopic(@Value("${kafka.topics.processing-completed}") String topic) {
        return TopicBuilder.name(topic).partitions(1).replicas(1).build();
    }

    @Bean
    NewTopic processingFailedTopic(@Value("${kafka.topics.processing-failed}") String topic) {
        return TopicBuilder.name(topic).partitions(1).replicas(1).build();
    }
}
