package com.Shawn.dream_shops.service.image;

import com.Shawn.dream_shops.dto.ImageDto;
import com.Shawn.dream_shops.exceptions.ProductNotFoundException;
import com.Shawn.dream_shops.exceptions.ResourceNotFoundException;
import com.Shawn.dream_shops.model.Image;
import com.Shawn.dream_shops.repository.ImageRepository;
import com.Shawn.dream_shops.repository.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Slf4j
public class ImageService implements IImageService {
    private final ImageRepository imageRepo;
    private final ProductRepository productRepo;
    private final ImageObjectStore store;
    private final ImageObjectJanitor janitor;
    private final String apiPrefix;

    ImageService(ImageRepository imageRepo, ProductRepository productRepo, ImageObjectStore store,
                 ImageObjectJanitor janitor, @Value("${api.prefix}") String apiPrefix) {
        this.imageRepo = imageRepo;
        this.productRepo = productRepo;
        this.store = store;
        this.janitor = janitor;
        this.apiPrefix = apiPrefix;
    }

    @Override
    @Transactional
    public ImageDownload downloadImage(Long id) {
        Image image = imageRepo.findForDownload(id).orElseThrow(() -> missing(id));
        byte[] bytes;
        if (image.getObjectKey() == null) {
            if (image.getImage() == null) {
                throw missing(id);
            }
            try {
                bytes = image.getImage().getBytes(1, Math.toIntExact(image.getImage().length()));
            } catch (SQLException ex) {
                throw new ImageStorageException(ex);
            }
        } else {
            if (!store.location().equals(image.getObjectStore())) {
                throw new ImageStorageException(new IllegalStateException("Configured image store differs from metadata"));
            }
            bytes = store.read(image.getObjectKey()).orElseThrow(() -> missing(id));
            if (!Objects.equals(image.getByteSize(), (long) bytes.length)) {
                throw new ImageStorageException(new IllegalStateException("Image size differs from metadata"));
            }
        }
        return new ImageDownload(image.getFileName(), image.getFileType(), bytes);
    }

    @Override
    @Transactional
    public void deleteImageById(Long id) {
        Image image = imageRepo.findForUpdate(id).orElseThrow(() -> missing(id));
        janitor.queueRemoval(image);
        imageRepo.delete(image);
    }

    @Override
    @Transactional
    public List<ImageDto> saveImages(List<MultipartFile> files, Long productID) {
        var uploads = files.stream().map(ImageUpload::validate).toList();
        var product = productRepo.findByIdForUpdate(productID)
                .orElseThrow(() -> new ProductNotFoundException("Product not found"));
        List<ImageDto> result = new ArrayList<>();
        for (ImageUpload upload : uploads) {
            Image image = new Image();
            image.setProduct(product);
            storeUpload(image, upload);
            image = imageRepo.save(image);
            image.setDownloadUrl(apiPrefix + "/images/image/download/" + image.getId());

            ImageDto dto = new ImageDto();
            dto.setImageID(image.getId());
            dto.setImageName(image.getFileName());
            dto.setDownloadURL(image.getDownloadUrl());
            result.add(dto);
        }
        return result;
    }

    @Override
    @Transactional
    public void updateImage(MultipartFile file, Long imageID) {
        ImageUpload upload = ImageUpload.validate(file);
        Image image = imageRepo.findForUpdate(imageID).orElseThrow(() -> missing(imageID));
        janitor.queueRemoval(image);
        storeUpload(image, upload);
    }

    private void storeUpload(Image image, ImageUpload upload) {
        String key = "product-images/" + UUID.randomUUID();
        store.put(key, upload.contentType(), upload.bytes());
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    try {
                        store.delete(key);
                    } catch (ImageStorageException ex) {
                        log.warn("Could not remove rolled-back product image {}", key);
                    }
                }
            }
        });
        image.setObjectKey(key);
        image.setObjectStore(store.location());
        image.setByteSize((long) upload.bytes().length);
        image.setFileType(upload.contentType());
        image.setFileName(upload.fileName());
    }

    private static ResourceNotFoundException missing(Long id) {
        return new ResourceNotFoundException("No image data found with id: " + id);
    }
}
