package com.mddev.processingservice.consumer;

import com.mddev.processingservice.event.VideoUploadedEvent;
import com.mddev.processingservice.service.ProcessingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class VideoUploadedConsumer {

    private final ProcessingService processingService;
    private final String topic;

    public VideoUploadedConsumer(ProcessingService processingService,
                                 @Value("${kafka.topics.video-uploaded}") String topic) {
        this.processingService = processingService;
        this.topic = topic;
    }

    @KafkaListener(
            topics = "#{__listener.topic}",
            containerFactory = "uploadedEventKafkaListenerContainerFactory"
    )
    public void consume(VideoUploadedEvent event) {
        processingService.processUploadedVideo(event);
    }

    public String getTopic() {
        return topic;
    }
}
