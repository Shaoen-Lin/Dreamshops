package com.Shawn.dream_shops.repository;

import com.Shawn.dream_shops.model.ImageObjectCleanup;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.time.Instant;
import java.util.List;

public interface ImageObjectCleanupRepository extends JpaRepository<ImageObjectCleanup, String> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<ImageObjectCleanup> findTop20ByObjectStoreAndRetryAtBeforeOrderByRetryAtAscObjectKeyAsc(
            String objectStore, Instant now);
}
