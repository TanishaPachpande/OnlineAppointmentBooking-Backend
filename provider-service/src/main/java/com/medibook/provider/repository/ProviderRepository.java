package com.medibook.provider.repository;

import com.medibook.provider.entity.Provider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProviderRepository extends JpaRepository<Provider, Long> {

    Optional<Provider> findByUserId(Long userId);

    List<Provider> findBySpecializationIgnoreCase(String specialization);

    List<Provider> findByIsVerified(Boolean isVerified);

    List<Provider> findByIsAvailable(Boolean isAvailable);

    List<Provider> findByClinicAddressContainingIgnoreCase(String clinicAddress);

    List<Provider> findByClinicNameContainingIgnoreCaseOrSpecializationContainingIgnoreCase(
            String clinicName, String specialization
    );
}