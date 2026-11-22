package com.Shawn.dream_shops.service.image;

import com.Shawn.dream_shops.dto.ImageDto;
import com.Shawn.dream_shops.exceptions.ProductNotFoundException;
import com.Shawn.dream_shops.exceptions.ResourceNotFoundException;
import com.Shawn.dream_shops.model.Image;
import com.Shawn.dream_shops.model.Product;
import com.Shawn.dream_shops.repository.ImageRepository;
import com.Shawn.dream_shops.repository.ProductRepository;
import com.Shawn.dream_shops.service.product.ProductService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.sql.rowset.serial.SerialBlob;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Service
//@RequiredArgsConstructor
public class ImageService implements IImageService{

    @Autowired
    ImageRepository imageRepo;

    @Autowired
    ProductRepository prodRepo;

    @Value("${api.prefix}")
    private String apiPrefix;

    @Override
    public Image getImageById(Long id) {
        return imageRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("No image found with id: " + id));
    }

    @Override
    public void deleteImageById(Long id) {
        imageRepo.findById(id).ifPresentOrElse(
                i -> imageRepo.delete(i),
                () -> {throw new ResourceNotFoundException("No image found with id: " + id);}
            );
    }

    @Override
    @Transactional
    public List<ImageDto> saveImages(List<MultipartFile> files, Long productID) {

        Product product = prodRepo
                                .findById(productID)
                                .orElseThrow(() -> new ProductNotFoundException("Product Not Found"));

        List<ImageDto> savedImageDto = new ArrayList<>();
        for(MultipartFile file : files)
        {
            try{
                Image image = new Image();

                image.setFileName(file.getOriginalFilename());
                image.setFileType(file.getContentType());
                image.setImage(new SerialBlob(file.getBytes())); // TypeCasting to Blob
                image.setProduct(product);

                Image savedImage = imageRepo.save(image);
                savedImage.setDownloadUrl(apiPrefix + "/images/image/download/" + savedImage.getId());

                // 建構對應的 DTO 回傳
                ImageDto imageDto = new ImageDto();
                imageDto.setImageID(savedImage.getId());
                imageDto.setImageName(savedImage.getFileName());
                imageDto.setDownloadURL(savedImage.getDownloadUrl());
                savedImageDto.add(imageDto);

            } catch(IOException | SQLException e)
            {
                throw new RuntimeException(e.getMessage());
            }
        }
        return savedImageDto;
    }

    @Override
    public void updateImage(MultipartFile file, Long imageID) {
        Image image = getImageById(imageID);
        try{
            image.setFileName(file.getOriginalFilename());
            image.setFileType(file.getContentType());
            image.setImage(new SerialBlob(file.getBytes())); // TypeCasting to Blob

            imageRepo.save(image);
        } catch(IOException | SQLException e)
        {
            throw new RuntimeException(e.getMessage());
        }
        // Exception(e.getMessage()) 會丟失錯誤堆疊資訊（stack trace） 這樣你只留下一個字串訊息，
        // 看不到是哪一行出錯、原本是哪個 Exception 類型。
    }
}
