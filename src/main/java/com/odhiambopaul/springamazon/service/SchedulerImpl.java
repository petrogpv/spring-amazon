package com.odhiambopaul.springamazon.service;

import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SchedulerImpl implements Scheduler {

    @Value("${aws.sns.topic.arn}")
    private String topicArn;

    @Value("${aws.sqs.url}")
    private String sqsUrl;

    private final AmazonSNSClient amazonSNSClient;
    private final AmazonSQSClient amazonSQSClient;


    @Scheduled(cron = "0 0/1 * * * ?")
    void processQueueMessagesToSns() {
        log.info("Scheduler is running");
        ReceiveMessageResult receiveMessageResult = amazonSQSClient.receiveMessage(sqsUrl);
        receiveMessageResult.getMessages().stream()
                .peek(m -> amazonSQSClient.deleteMessage(sqsUrl, m.getReceiptHandle()))
                .map(Message::getBody)
                .peek(m -> log.info("Message received {}", m))
                .forEach(b -> amazonSNSClient.publish(new PublishRequest(topicArn, b, "New image has been uploaded")));
    }
}
