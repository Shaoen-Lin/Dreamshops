package com.Shawn.dream_shops.repository;

import com.Shawn.dream_shops.model.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);

    @Modifying
    @Query("""
            update Product p set p.inventory = p.inventory - :quantity
            where p.id = :id and p.inventory >= :quantity
            """)
    int decrementAvailableInventory(@Param("id") Long id, @Param("quantity") int quantity);

    boolean existsByName(String name);

    List<Product> findByCategoryName(String category);
    // 這裡會發現如果 直接寫 findByCategory 會發現 JPA 會以為我們要給他 category 物件 
    // 但我們其實要給的是一個string 所以名稱改成 findByCategoryName

    List<Product> findByBrand(String brand);

    List<Product> findByCategoryNameAndBrand(String category, String brand);

    List<Product> findByName(String name);

    List<Product> findByBrandAndName(String brand, String name);

    Long countByBrandAndName(String brand, String name);

    boolean existsByNameAndBrand(String name, String brand);
}
