package com.Shawn.dream_shops.service.image;

import com.Shawn.dream_shops.exceptions.ResourceNotFoundException;
import com.Shawn.dream_shops.model.Image;
import com.Shawn.dream_shops.model.Product;
import com.Shawn.dream_shops.repository.ImageRepository;
import com.Shawn.dream_shops.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import javax.sql.rowset.serial.SerialBlob;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ImageServiceTest {
    private final ImageRepository images = mock(ImageRepository.class);
    private final ProductRepository products = mock(ProductRepository.class);
    private final ImageObjectStore store = mock(ImageObjectStore.class);
    private final ImageObjectJanitor janitor = mock(ImageObjectJanitor.class);
    private final ImageService service = new ImageService(images, products, store, janitor, "/api/v1");

    @Test
    void recordsMetadataAndCleansUncommittedObjectWithoutDeletingCommittedObjects() throws Exception {
        when(products.findByIdForUpdate(8L)).thenReturn(Optional.of(new Product()));
        when(store.location()).thenReturn("gcs:private-test");
        when(images.save(any(Image.class))).thenAnswer(invocation -> {
            Image image = invocation.getArgument(0);
            image.setId(4L);
            return image;
        });
        TransactionSynchronizationManager.initSynchronization();
        try {
            var uploaded = service.saveImages(List.of(png()), 8L).getFirst();
            var captor = org.mockito.ArgumentCaptor.forClass(Image.class);
            verify(images).save(captor.capture());
            Image image = captor.getValue();
            assertThat(image.getObjectKey()).matches("product-images/[0-9a-f-]{36}");
            assertThat(image.getImage()).isNull();
            assertThat(image.getByteSize()).isPositive();
            assertThat(image.getFileType()).isEqualTo("image/png");
            assertThat(uploaded.getImageName()).isEqualTo("photo.png");
            assertThat(uploaded.getDownloadURL()).isEqualTo("/api/v1/images/image/download/4");
            verify(store, never()).delete(anyString());
            var cleanup = TransactionSynchronizationManager.getSynchronizations().getFirst();
            cleanup.afterCompletion(TransactionSynchronization.STATUS_UNKNOWN);
            verify(store, never()).delete(anyString());
            cleanup.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
            verify(store).delete(image.getObjectKey());
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void refusesInvalidAndOversizedFilesBeforeWriting() {
        var invalid = new MockMultipartFile("files", "fake.png", "image/png", new byte[]{1, 2});
        var oversized = new MockMultipartFile("files", "large.png", "image/png", new byte[5 * 1024 * 1024 + 1]);
        assertThatThrownBy(() -> service.saveImages(List.of(invalid), 1L))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode().value()).isEqualTo(415));
        assertThatThrownBy(() -> service.saveImages(List.of(oversized), 1L))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode().value()).isEqualTo(413));
        verifyNoInteractions(store, images);
    }

    @Test
    void storageFailureDoesNotSaveMetadata() throws Exception {
        when(products.findByIdForUpdate(8L)).thenReturn(Optional.of(new Product()));
        doThrow(new ImageStorageException(new IOException())).when(store).put(anyString(), anyString(), any());
        assertThatThrownBy(() -> service.saveImages(List.of(png()), 8L))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode().value()).isEqualTo(503));
        verifyNoInteractions(images);
    }

    @Test
    void readsLegacyBlobWithoutContactingStore() throws Exception {
        var image = new Image();
        image.setFileType("image/jpeg");
        image.setImage(new SerialBlob(new byte[]{1, 2, 3}));
        when(images.findForDownload(1L)).thenReturn(Optional.of(image));
        assertThat(service.downloadImage(1L).bytes()).containsExactly(1, 2, 3);
        verifyNoInteractions(store);
    }

    @Test
    void downloadsStoredBytesAndDistinguishesMissingObjectsFromStorageFailures() {
        var image = new Image();
        image.setObjectKey("product-images/test");
        image.setObjectStore("gcs:private-test");
        image.setByteSize(2L);
        when(images.findForDownload(1L)).thenReturn(Optional.of(image));
        when(store.location()).thenReturn("gcs:private-test");
        when(store.read(image.getObjectKey())).thenReturn(Optional.of(new byte[]{1, 2}), Optional.empty())
                .thenThrow(new ImageStorageException(new IOException()));
        assertThat(service.downloadImage(1L).bytes()).containsExactly(1, 2);
        assertThatThrownBy(() -> service.downloadImage(1L)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.downloadImage(1L)).isInstanceOf(ImageStorageException.class);
    }

    @Test
    void deletionOnlyQueuesObjectRemovalWithinMetadataTransaction() {
        var image = new Image();
        when(images.findForUpdate(1L)).thenReturn(Optional.of(image));
        service.deleteImageById(1L);
        verify(janitor).queueRemoval(image);
        verify(images).delete(image);
        verifyNoInteractions(store);
    }

    private MockMultipartFile png() throws IOException {
        var bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", bytes);
        return new MockMultipartFile("files", "../photo.png", "text/plain", bytes.toByteArray());
    }
}
