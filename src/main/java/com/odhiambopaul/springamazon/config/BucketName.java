package com.odhiambopaul.springamazon.config;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum BucketName {
    TODO_IMAGE("cloudx-spring-amazon-storage");
    private final String bucketName;
}
