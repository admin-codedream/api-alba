package com.api.alba.component;

import com.api.alba.firebase.ProjectId;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Component
public class FcmInitializer {

    private final static String FIREBASE_CONFIG_PATH = "/fcm/albam-firebase-adminsdk.json";

    @PostConstruct
    private void getFcmCredential() {
        try {
            InputStream account = new ClassPathResource(
                    FIREBASE_CONFIG_PATH).getInputStream();

            FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(account))
                .build();
            FirebaseApp.initializeApp(options, ProjectId.ALBAM.getMessage());

            log.info("=======================================");
            log.info(":: Firebase initialization completed ::");
            log.info("=======================================");
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
    }
}
