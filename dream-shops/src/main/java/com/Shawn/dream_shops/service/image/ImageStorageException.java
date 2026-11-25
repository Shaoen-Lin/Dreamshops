package com.Shawn.dream_shops.service.image;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class ImageStorageException extends ResponseStatusException {
    ImageStorageException(Throwable cause) {
        super(HttpStatus.SERVICE_UNAVAILABLE, "Image storage is temporarily unavailable", cause);
    }
}
