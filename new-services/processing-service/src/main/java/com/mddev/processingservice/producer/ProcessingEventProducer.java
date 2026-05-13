package com.mddev.processingservice.producer;

import com.mddev.processingservice.event.VideoProcessingCompletedEvent;
import com.mddev.processingservice.event.VideoProcessingFailedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class ProcessingEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String completedTopic;
    private final String failedTopic;

    public ProcessingEventProducer(KafkaTemplate<String, Object> kafkaTemplate,
                                   @Value("${kafka.topics.processing-completed}") String completedTopic,
                                   @Value("${kafka.topics.processing-failed}") String failedTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.completedTopic = completedTopic;
        this.failedTopic = failedTopic;
    }

    public void publishCompleted(VideoProcessingCompletedEvent event) {
        kafkaTemplate.send(completedTopic, event.videoId().toString(), event);
    }

    public void publishFailed(VideoProcessingFailedEvent event) {
        kafkaTemplate.send(failedTopic, event.videoId().toString(), event);
    }
}
