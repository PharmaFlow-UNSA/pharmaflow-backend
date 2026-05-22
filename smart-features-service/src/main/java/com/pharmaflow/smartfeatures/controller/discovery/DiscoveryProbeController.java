package com.pharmaflow.smartfeatures.controller.discovery;

import com.pharmaflow.smartfeatures.dto.discovery.DiscoveryProbeResponseDto;
import com.pharmaflow.smartfeatures.service.discovery.DiscoveryProbeService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/discovery")
public class DiscoveryProbeController {

  private final DiscoveryProbeService discoveryProbeService;

  public DiscoveryProbeController(DiscoveryProbeService discoveryProbeService) {
    this.discoveryProbeService = discoveryProbeService;
  }

  @GetMapping("/{serviceKey}/health")
  @PreAuthorize("hasRole('ADMIN')")
  public DiscoveryProbeResponseDto probeHealth(
      @PathVariable String serviceKey, @RequestParam(defaultValue = "load-balanced") String mode) {
    return discoveryProbeService.probe(serviceKey, mode);
  }
}
