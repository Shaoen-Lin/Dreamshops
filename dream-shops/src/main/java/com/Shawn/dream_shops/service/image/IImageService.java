package com.Shawn.dream_shops.service.image;

import com.Shawn.dream_shops.dto.ImageDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IImageService {
    record ImageDownload(String fileName, String contentType, byte[] bytes) {}

    ImageDownload downloadImage(Long id);
    void deleteImageById(Long id);
    List<ImageDto> saveImages(List<MultipartFile> files, Long productID);
    void updateImage(MultipartFile file, Long imageID);
}
