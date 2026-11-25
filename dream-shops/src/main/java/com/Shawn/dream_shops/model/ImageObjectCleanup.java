package com.Shawn.dream_shops.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ImageObjectCleanup {
    @Id
    private String objectKey;

    private String objectStore;
    private Instant retryAt = Instant.EPOCH;

    public ImageObjectCleanup(String objectKey, String objectStore) {
        this.objectKey = objectKey;
        this.objectStore = objectStore;
    }

    public void retryAt(Instant time) {
        this.retryAt = time;
    }
}
