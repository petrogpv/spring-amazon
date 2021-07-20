package com.odhiambopaul.springamazon.service;

import com.odhiambopaul.springamazon.domain.Image;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ImageService {
    Image uploadImage(String description, MultipartFile file);

    byte[] downloadImageByName(String imageName);

    Long deleteImageByName(String imageName);

    List<Image> getAllImages();

    byte[] getRandomImage();

    SubscribeResult subscribeEmail(String email);

    UnsubscribeResult unsubscribeEmail(String email);
}
