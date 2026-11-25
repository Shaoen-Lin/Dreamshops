package com.Shawn.dream_shops.service.image;

import com.Shawn.dream_shops.model.ImageObjectCleanup;
import com.Shawn.dream_shops.repository.ImageObjectCleanupRepository;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ImageObjectJanitorTest {
    @Test
    void retainsFailedCleanupForRetryAndRemovesSuccessfulEntry() {
        var repo = mock(ImageObjectCleanupRepository.class);
        var store = mock(ImageObjectStore.class);
        var failed = new ImageObjectCleanup("product-images/failed", "gcs:private-test");
        var removed = new ImageObjectCleanup("product-images/removed", "gcs:private-test");
        when(store.location()).thenReturn("gcs:private-test");
        when(repo.findTop20ByObjectStoreAndRetryAtBeforeOrderByRetryAtAscObjectKeyAsc(
                eq("gcs:private-test"), any())).thenReturn(List.of(failed, removed));
        doThrow(new ImageStorageException(new IOException())).when(store).delete(failed.getObjectKey());

        new ImageObjectJanitor(repo, store).processRemovals();

        assertThat(failed.getRetryAt()).isAfter(Instant.now());
        verify(repo, never()).delete(failed);
        verify(repo).delete(removed);
    }
}
