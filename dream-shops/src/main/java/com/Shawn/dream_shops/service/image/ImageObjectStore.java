package com.Shawn.dream_shops.service.image;

import java.util.Optional;

interface ImageObjectStore {
    String location();
    void put(String key, String contentType, byte[] bytes);
    Optional<byte[]> read(String key);
    void delete(String key);
}
