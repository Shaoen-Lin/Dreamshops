package com.Shawn.dream_shops.service.image;

import com.Shawn.dream_shops.model.Image;
import com.Shawn.dream_shops.model.ImageObjectCleanup;
import com.Shawn.dream_shops.repository.ImageObjectCleanupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageObjectJanitor {
    private final ImageObjectCleanupRepository cleanupRepo;
    private final ImageObjectStore store;

    @Transactional(propagation = Propagation.MANDATORY)
    public void queueRemoval(Image image) {
        if (image.getObjectKey() != null) {
            cleanupRepo.save(new ImageObjectCleanup(image.getObjectKey(), image.getObjectStore()));
        }
    }

    @Scheduled(fixedDelayString = "${shop.images.cleanup-delay-ms}", initialDelayString = "${shop.images.cleanup-delay-ms}")
    @Transactional
    public void processRemovals() {
        Instant now = Instant.now();
        for (ImageObjectCleanup entry : cleanupRepo.findTop20ByObjectStoreAndRetryAtBeforeOrderByRetryAtAscObjectKeyAsc(
                store.location(), now)) {
            try {
                store.delete(entry.getObjectKey());
                cleanupRepo.delete(entry);
            } catch (ImageStorageException ex) {
                entry.retryAt(now.plus(5, ChronoUnit.MINUTES));
                log.warn("Product image cleanup failed for {}; will retry", entry.getObjectKey());
            }
        }
    }
}
