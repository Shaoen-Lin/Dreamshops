package com.Shawn.dream_shops.service.image;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;

import java.util.Optional;

final class GcsImageObjectStore implements ImageObjectStore {
    private final Storage storage;
    private final String bucket;

    GcsImageObjectStore(Storage storage, String bucket) {
        this.storage = storage;
        this.bucket = bucket;
    }

    @Override
    public String location() {
        return "gcs:" + bucket;
    }

    @Override
    public void put(String key, String contentType, byte[] bytes) {
        try {
            var info = BlobInfo.newBuilder(BlobId.of(bucket, key)).setContentType(contentType).build();
            storage.create(info, bytes, Storage.BlobTargetOption.doesNotExist());
        } catch (StorageException ex) {
            throw new ImageStorageException(ex);
        }
    }

    @Override
    public Optional<byte[]> read(String key) {
        try {
            return Optional.of(storage.readAllBytes(BlobId.of(bucket, key)));
        } catch (StorageException ex) {
            if (ex.getCode() == 404) {
                return Optional.empty();
            }
            throw new ImageStorageException(ex);
        }
    }

    @Override
    public void delete(String key) {
        try {
            storage.delete(BlobId.of(bucket, key));
        } catch (StorageException ex) {
            if (ex.getCode() != 404) {
                throw new ImageStorageException(ex);
            }
        }
    }
}
