package com.Shawn.dream_shops.service.image;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class GcsImageObjectStoreTest {
    private final Storage storage = mock(Storage.class);
    private final BlobId id = BlobId.of("private-test", "product-images/key");
    private final GcsImageObjectStore store = new GcsImageObjectStore(storage, id.getBucket());

    @Test
    void uploadsWithCreateOnlyPrecondition() {
        byte[] bytes = {1, 2, 3};
        store.put(id.getName(), "image/png", bytes);
        verify(storage).create(BlobInfo.newBuilder(id).setContentType("image/png").build(),
                bytes, Storage.BlobTargetOption.doesNotExist());
    }

    @Test
    void mapsMissingObjectsAndPermissionErrors() {
        when(storage.readAllBytes(id)).thenThrow(new StorageException(404, "missing"))
                .thenThrow(new StorageException(403, "forbidden"));
        assertThat(store.read(id.getName())).isEmpty();
        assertThatThrownBy(() -> store.read(id.getName())).isInstanceOf(ImageStorageException.class);
        when(storage.delete(id)).thenReturn(false).thenThrow(new StorageException(403, "forbidden"));
        store.delete(id.getName());
        assertThatThrownBy(() -> store.delete(id.getName())).isInstanceOf(ImageStorageException.class);
    }
}
