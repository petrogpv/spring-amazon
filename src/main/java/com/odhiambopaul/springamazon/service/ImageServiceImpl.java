package com.odhiambopaul.springamazon.service;

import static org.apache.http.entity.ContentType.IMAGE_BMP;
import static org.apache.http.entity.ContentType.IMAGE_GIF;
import static org.apache.http.entity.ContentType.IMAGE_JPEG;
import static org.apache.http.entity.ContentType.IMAGE_PNG;

import com.amazonaws.services.lambda.AWSLambdaClient;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.model.InvokeResult;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.ListSubscriptionsResult;
import com.amazonaws.services.sns.model.SubscribeRequest;
import com.amazonaws.services.sns.model.SubscribeResult;
import com.amazonaws.services.sns.model.Subscription;
import com.amazonaws.services.sns.model.UnsubscribeRequest;
import com.amazonaws.services.sns.model.UnsubscribeResult;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odhiambopaul.springamazon.domain.Image;
import com.odhiambopaul.springamazon.repositories.ImageRepository;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ImageServiceImpl implements ImageService {

  private final FileStoreServiceImpl fileStore;
  private final ImageRepository repository;
  private final AmazonSNSClient amazonSNSClient;
  private final AmazonSQSClient amazonSQSClient;
  private final AWSLambdaClient awsLambdaClient;
  private final ObjectMapper mapper;


  @Value("${aws.s3.bucketName}")
  private String bucketName;

  @Value("${aws.sns.topic.arn}")
  private String topicArn;

  @Value("${aws.sqs.url}")
  private String sqsUrl;

  @Value("aws.lambda.arn")
  private String lambdaArn;

  @Override
  public Image uploadImage(String description, MultipartFile file) {

    if (file.isEmpty()) {
      throw new IllegalStateException("Cannot upload empty file");
    }

    if (!Arrays.asList(IMAGE_PNG.getMimeType(),
        IMAGE_BMP.getMimeType(),
        IMAGE_GIF.getMimeType(),
        IMAGE_JPEG.getMimeType()).contains(file.getContentType())) {
      throw new IllegalStateException("FIle uploaded is not an image");
    }

    Map<String, String> metadata = new HashMap<>();
    metadata.put("Content-Type", file.getContentType());
    metadata.put("Content-Length", String.valueOf(file.getSize()));

    String path = String.format("%s/%s", bucketName, "images");
    try {
      String url = fileStore.upload(path, file.getOriginalFilename(), Optional.of(metadata), file.getInputStream());

      Image image = Image.builder()
          .description(description)
          .imagePath(path)
          .imageFileName(file.getOriginalFilename())
          .imageExtension(FilenameUtils.getExtension(file.getOriginalFilename()))
              .url(url)
          .imageSize(file.getSize())
          .updateDate(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
          .build();

      repository.save(image);

      amazonSQSClient.sendMessage(new SendMessageRequest(sqsUrl, mapper.writeValueAsString(image)));

      return repository.findByImageFileName(image.getImageFileName());
    } catch (RuntimeException | IOException e) {
      log.error("Failed to upload file, {}", e.getMessage());
      throw new IllegalStateException("Failed to upload file", e);
    }
  }

  @Override
  public byte[] downloadImageByName(String imageName) {
    try {
      Image image = repository.findByImageFileName(imageName);
      byte[] download = fileStore.download(image.getImagePath(), image.getImageFileName());
      log.info("downloaded image, {} ", image.getImageFileName());

      return download;
    } catch (Exception e) {
      log.error("Failed to download file, {}", e.getMessage());
      throw e;
    }
  }

  @Override
  public Long deleteImageByName(String imageName) {
    try {
      Image image = repository.findByImageFileName(imageName);
      fileStore.delete(image.getImagePath(), image.getImageFileName());
      Long aLong = repository.deleteByImageFileName(imageName);
      log.info("deleted image, {}", image);

      return aLong;
    } catch (Exception e) {
      log.error("Error deleting image, {}", e.getMessage());
      throw e;
    }
  }

  @Override
  public List<Image> getAllImages() {
    try {
      List<Image> images = new ArrayList<>();
      repository.findAll().forEach(images::add);
      log.info("Images founded, {}", images);

      return images;
    } catch (Exception e) {
      log.error("Error deleting image, {}", e.getMessage());
      throw e;
    }
  }

  @Override
  public byte[] getRandomImage() {
    try {
      List<Image> list = getAllImages();
      Image image = list.get(new Random().nextInt(list.size()));
      byte[] download = fileStore.download(image.getImagePath(), image.getImageFileName());
      log.info("Random image founded, {}", image.getImageFileName());

      return download;
    } catch (Exception e) {
      log.error("Error getting random image, {}", e.getMessage());
      throw e;
    }
  }

  @Override
  public SubscribeResult subscribeEmail(String email) {
    try {
      final SubscribeRequest subscribeRequest = new SubscribeRequest(topicArn, "email", email);
      SubscribeResult subscribe = amazonSNSClient.subscribe(subscribeRequest);
      log.info("Subscribed with arn, {}", subscribe.getSubscriptionArn());

      return subscribe;
    } catch (Exception e) {
      log.error("Error subscribing, {}", e.getMessage());
      return new SubscribeResult();
    }
  }

  @Override
  public UnsubscribeResult unsubscribeEmail(String email) {
    try {
      ListSubscriptionsResult listResult = amazonSNSClient.listSubscriptions();
      List<Subscription> subscriptions = listResult.getSubscriptions();
      String arn = subscriptions.stream()
              .filter(subs -> subs.getProtocol().equalsIgnoreCase("email") && subs.getEndpoint().equals(email))
              .findFirst()
              .map(Subscription::getSubscriptionArn)
              .orElse("");

      final UnsubscribeRequest unSubscribeRequest = new UnsubscribeRequest(arn);
      UnsubscribeResult unsubscribe = amazonSNSClient.unsubscribe(unSubscribeRequest);
      log.info("Unsubscribed arn, {}", arn);

      return unsubscribe;
    } catch (Exception e) {
      log.error("Error unsubscribing, {}", e.getMessage());
      return new UnsubscribeResult();
    }
  }

  @Override
  public String triggerLambda() {
    try {
      InvokeRequest invokeRequest = new InvokeRequest()
          .withFunctionName(lambdaArn)
          .withPayload("{\n \"Hello \": \"Paris\",\n}");
      log.info("invoke lambda {} request, {}", lambdaArn, invokeRequest.toString());

      InvokeResult invokeResult = awsLambdaClient.invoke(invokeRequest);
      log.info("invoke lambda {} result, {}", lambdaArn, invokeResult.toString());

      return invokeResult.toString();
    } catch (Exception e) {
      log.error(e.getMessage());
      return e.getMessage();
    }

  }
}
