package com.capstone.rentit.notification.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;

@Profile("!test")
@Configuration
public class FirebaseConfig {

    @Bean
    public FirebaseApp firebaseApp() throws IOException {

        ClassPathResource res = new ClassPathResource("firebase/rentit-5b36b-firebase-adminsdk-fbsvc-ab4f4216ef.json");
        InputStream credentialStream = res.getInputStream();

        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(credentialStream))
                .build();

        return FirebaseApp.initializeApp(options);
    }
}