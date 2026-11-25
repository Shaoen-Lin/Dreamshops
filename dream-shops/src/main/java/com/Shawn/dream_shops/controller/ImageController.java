package com.Shawn.dream_shops.controller;

import com.Shawn.dream_shops.reponse.ApiResponse;
import com.Shawn.dream_shops.service.image.IImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@RequiredArgsConstructor
@RestController
@RequestMapping("${api.prefix}/images")
public class ImageController {
    private static final Set<String> RASTER_MEDIA_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp", "image/avif", "image/bmp"
    );
    private final IImageService imageService;

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse> saveImages(@RequestParam List<MultipartFile> files, @RequestParam Long productID) {
        return ResponseEntity.ok(new ApiResponse("Upload success!", imageService.saveImages(files, productID)));
    }

    @GetMapping("/image/download/{imageID}")
    public ResponseEntity<Resource> downloadImages(@PathVariable Long imageID) {
        var image = imageService.downloadImage(imageID);
        String mediaType = image.contentType() == null ? "" : image.contentType().toLowerCase(Locale.ROOT);
        String fileName = image.fileName() == null ? "download" : image.fileName();
        return ResponseEntity.ok()
                .contentType(RASTER_MEDIA_TYPES.contains(mediaType)
                        ? MediaType.parseMediaType(mediaType) : MediaType.APPLICATION_OCTET_STREAM)
                .header("Content-Security-Policy", "sandbox; default-src 'none'")
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(fileName, StandardCharsets.UTF_8).build().toString())
                .body(new ByteArrayResource(image.bytes()));
    }

    @PostMapping("/image/{imageID}/update")
    public ResponseEntity<ApiResponse> updateImage(@PathVariable Long imageID, @RequestPart MultipartFile file) {
        imageService.updateImage(file, imageID);
        return ResponseEntity.ok(new ApiResponse("Update Success!", null));
    }

    @DeleteMapping("/image/{imageID}/update")
    public ResponseEntity<ApiResponse> deleteImage(@PathVariable Long imageID) {
        imageService.deleteImageById(imageID);
        return ResponseEntity.ok(new ApiResponse("Delete Success!", null));
    }

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ApiResponse> handleImageRequestFailure(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(new ApiResponse(ex.getReason(), null));
    }
}
