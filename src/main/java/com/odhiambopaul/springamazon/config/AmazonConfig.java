package com.odhiambopaul.springamazon.config;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class AmazonConfig {

    @Value("${aws.accessKeyID}")
    private String accessKeyId;
    @Value("${aws.secretAccessKey}")
    private String secretAccessKey;


    @Bean
    @Profile("!local")
    public AmazonS3 s3() {
        return AmazonS3ClientBuilder
                .standard()
                .withRegion("us-west-1")
                .build();
    }

    @Bean
    @Profile("local")
    public AmazonS3 s3local() {
        AWSCredentials awsCredentials =
                new BasicAWSCredentials(accessKeyId, secretAccessKey);
        return AmazonS3ClientBuilder
                .standard()
                .withRegion("us-west-1")
                .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
                .build();

    }
}
