package com.Shawn.dream_shops.repository;

import com.Shawn.dream_shops.model.Image;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ImageRepository extends JpaRepository<Image, Long> {
    List<Image> findByProductId(Long id);

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select i from Image i where i.id = :id")
    Optional<Image> findForDownload(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Image i where i.id = :id")
    Optional<Image> findForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Image i where i.product.id = :productId order by i.id")
    List<Image> findProductImagesForUpdate(@Param("productId") Long productId);
}
