package com.mddev.videoservice.consumer;

import com.mddev.videoservice.event.VideoProcessingCompletedEvent;
import com.mddev.videoservice.service.VideoService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class VideoProcessingCompletedConsumer {

    private final VideoService videoService;
    private final String topic;

    public VideoProcessingCompletedConsumer(VideoService videoService,
                                            @Value("${kafka.topics.processing-completed}") String topic) {
        this.videoService = videoService;
        this.topic = topic;
    }

    @KafkaListener(
            topics = "#{__listener.topic}",
            containerFactory = "completedEventKafkaListenerContainerFactory"
    )
    public void consume(VideoProcessingCompletedEvent event) {
        videoService.markCompleted(event);
    }

    public String getTopic() {
        return topic;
    }
}
