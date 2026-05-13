package com.mddev.videoservice.producer;

import com.mddev.videoservice.event.VideoUploadedEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class VideoEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String videoUploadedTopic;

    public VideoEventProducer(KafkaTemplate<String, Object> kafkaTemplate,
                              @Value("${kafka.topics.video-uploaded}") String videoUploadedTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.videoUploadedTopic = videoUploadedTopic;
    }

    public void publishVideoUploaded(VideoUploadedEvent event) {
        kafkaTemplate.send(videoUploadedTopic, event.videoId().toString(), event);
    }
}
