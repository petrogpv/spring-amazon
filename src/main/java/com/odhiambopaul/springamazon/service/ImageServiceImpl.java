package com.odhiambopaul.springamazon.service;

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
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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

import static org.apache.http.entity.ContentType.IMAGE_BMP;
import static org.apache.http.entity.ContentType.IMAGE_GIF;
import static org.apache.http.entity.ContentType.IMAGE_JPEG;
import static org.apache.http.entity.ContentType.IMAGE_PNG;

@Service
@RequiredArgsConstructor
@Transactional
public class ImageServiceImpl implements ImageService {

  private final FileStoreServiceImpl fileStore;
  private final ImageRepository repository;
  private final AmazonSNSClient amazonSNSClient;
  private final AmazonSQSClient amazonSQSClient;
  private final ObjectMapper mapper;


  @Value("${aws.s3.bucketName}")
  private String bucketName;

  @Value("${aws.sns.topic.arn}")
  private String topicArn;

  @Value("${aws.sqs.url}")
  private String sqsUrl;

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
      throw new IllegalStateException("Failed to upload file", e);
    }
  }

  @Override
  public byte[] downloadImageByName(String imageName) {
    Image image = repository.findByImageFileName(imageName);
    return fileStore.download(image.getImagePath(), image.getImageFileName());
  }

  @Override
  public Long deleteImageByName(String imageName) {
    Image image = repository.findByImageFileName(imageName);
    fileStore.delete(image.getImagePath(), image.getImageFileName());
    return repository.deleteByImageFileName(imageName);
  }

  @Override
  public List<Image> getAllImages() {
    List<Image> todos = new ArrayList<>();
    repository.findAll().forEach(todos::add);
    return todos;
  }

  @Override
  public byte[] getRandomImage() {
    List<Image> list = getAllImages();
    Image image = list.get(new Random().nextInt(list.size()));
    return fileStore.download(image.getImagePath(), image.getImageFileName());
  }

  @Override
  public SubscribeResult subscribeEmail(String email) {
    final SubscribeRequest subscribeRequest = new SubscribeRequest(topicArn, "email", email);
    return amazonSNSClient.subscribe(subscribeRequest);
  }

  @Override
  public UnsubscribeResult unsubscribeEmail(String email) {
    ListSubscriptionsResult listResult = amazonSNSClient.listSubscriptions();
    List<Subscription> subscriptions = listResult.getSubscriptions();
    String arn = subscriptions.stream()
            .filter(subs -> subs.getProtocol().equalsIgnoreCase("email") && subs.getEndpoint().equals(email))
            .findFirst()
            .map(Subscription::getSubscriptionArn)
            .orElse("");

    final UnsubscribeRequest unSubscribeRequest = new UnsubscribeRequest(arn);
    return amazonSNSClient.unsubscribe(unSubscribeRequest);
  }
}
