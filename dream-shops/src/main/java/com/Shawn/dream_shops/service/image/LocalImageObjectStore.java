package com.Shawn.dream_shops.service.image;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

final class LocalImageObjectStore implements ImageObjectStore {
    private final Path root;

    LocalImageObjectStore(Path root) {
        this.root = root.toAbsolutePath().normalize();
    }

    @Override
    public String location() {
        return "local:" + root;
    }

    @Override
    public void put(String key, String contentType, byte[] bytes) {
        Path file = resolve(key);
        boolean created = false;
        try {
            Files.createDirectories(file.getParent());
            try (var output = Files.newOutputStream(file, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
                created = true;
                output.write(bytes);
            }
        } catch (IOException ex) {
            if (created) {
                try {
                    Files.deleteIfExists(file);
                } catch (IOException cleanupFailure) {
                    ex.addSuppressed(cleanupFailure);
                }
            }
            throw new ImageStorageException(ex);
        }
    }

    @Override
    public Optional<byte[]> read(String key) {
        try {
            return Optional.of(Files.readAllBytes(resolve(key)));
        } catch (NoSuchFileException ex) {
            return Optional.empty();
        } catch (IOException ex) {
            throw new ImageStorageException(ex);
        }
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException ex) {
            throw new ImageStorageException(ex);
        }
    }

    private Path resolve(String key) {
        Path file = root.resolve(key).normalize();
        if (!file.startsWith(root) || file.equals(root)) {
            throw new IllegalArgumentException("Invalid image object key");
        }
        return file;
    }
}
