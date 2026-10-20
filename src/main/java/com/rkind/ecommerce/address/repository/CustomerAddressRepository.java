
package com.rkind.ecommerce.address.repository;

import com.rkind.ecommerce.address.entity.CustomerAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerAddressRepository
        extends JpaRepository<CustomerAddress, Long> {

    List<CustomerAddress> findByUser_IdOrderByIsDefaultDescCreatedAtDesc(
            Long userId
    );

    Optional<CustomerAddress> findByIdAndUser_Id(
            Long id,
            Long userId
    );

    boolean existsByUser_Id(Long userId);
}
