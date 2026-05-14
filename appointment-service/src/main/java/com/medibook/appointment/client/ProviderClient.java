package com.medibook.appointment.client;

import com.medibook.appointment.dto.ProviderResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "PROVIDER-SERVICE")
public interface ProviderClient {

    @GetMapping("/providers/{providerId}")
    ProviderResponseDto getProviderById(@PathVariable("providerId") Long providerId);
}
