package com.Shawn.dream_shops.service.image;

import com.google.cloud.storage.StorageOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.nio.file.Path;

@Configuration
@EnableScheduling
class ImageStorageConfiguration {
    @Bean
    ImageObjectStore imageObjectStore(@Value("${shop.images.bucket}") String bucket,
                                      @Value("${shop.images.local-dir}") String localDir) {
        if (bucket.isBlank()) {
            return new LocalImageObjectStore(Path.of(localDir));
        }
        return new GcsImageObjectStore(StorageOptions.getDefaultInstance().getService(), bucket);
    }
}
