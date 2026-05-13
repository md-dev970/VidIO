package com.mddev.videoservice.consumer;

import com.mddev.videoservice.event.VideoProcessingFailedEvent;
import com.mddev.videoservice.service.VideoService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class VideoProcessingFailedConsumer {

    private final VideoService videoService;
    private final String topic;

    public VideoProcessingFailedConsumer(VideoService videoService,
                                         @Value("${kafka.topics.processing-failed}") String topic) {
        this.videoService = videoService;
        this.topic = topic;
    }

    @KafkaListener(
            topics = "#{__listener.topic}",
            containerFactory = "failedEventKafkaListenerContainerFactory"
    )
    public void consume(VideoProcessingFailedEvent event) {
        videoService.markFailed(event);
    }

    public String getTopic() {
        return topic;
    }
}
