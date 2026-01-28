package com.activitypub.listener;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

@SpringBootApplication
@EnableMongoAuditing
public class ActivityPubListenerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ActivityPubListenerApplication.class, args);
    }
}
