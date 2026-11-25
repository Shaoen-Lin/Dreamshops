package com.Shawn.dream_shops.service.image;

import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;

record ImageUpload(String fileName, String contentType, byte[] bytes) {
    private static final int MAX_BYTES = 5 * 1024 * 1024;
    private static final long MAX_PIXELS = 16_000_000;
    private static final Map<String, String> CONTENT_TYPES = Map.of(
            "jpeg", "image/jpeg", "png", "image/png", "gif", "image/gif", "bmp", "image/bmp");

    static ImageUpload validate(MultipartFile file) {
        if (file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image is empty");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Image exceeds 5 MiB");
        }
        try {
            byte[] bytes = file.getBytes();
            if (bytes.length > MAX_BYTES) {
                throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Image exceeds 5 MiB");
            }
            return new ImageUpload(safeFileName(file.getOriginalFilename()), inspect(bytes), bytes);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Invalid or unsupported image", ex);
        }
    }

    private static String inspect(byte[] bytes) throws IOException {
        try (var input = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Use JPEG, PNG, GIF, or BMP");
            }
            var reader = readers.next();
            try {
                String contentType = CONTENT_TYPES.get(reader.getFormatName().toLowerCase(Locale.ROOT));
                if (contentType == null) {
                    throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Use JPEG, PNG, GIF, or BMP");
                }
                reader.setInput(input, true, true);
                if ((long) reader.getWidth(0) * reader.getHeight(0) > MAX_PIXELS) {
                    throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Image exceeds 16 megapixels");
                }
                reader.read(0).flush();
                return contentType;
            } finally {
                reader.dispose();
            }
        }
    }

    private static String safeFileName(String original) {
        if (original == null) {
            return "image";
        }
        String path = original.replace('\\', '/');
        String name = path.substring(path.lastIndexOf('/') + 1).replaceAll("\\p{Cntrl}", "").strip();
        if (name.isEmpty()) {
            return "image";
        }
        int count = Math.min(name.codePointCount(0, name.length()), 180);
        return name.substring(0, name.offsetByCodePoints(0, count));
    }
}
