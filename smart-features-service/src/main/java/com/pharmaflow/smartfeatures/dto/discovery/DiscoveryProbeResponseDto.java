package com.pharmaflow.smartfeatures.dto.discovery;

public record DiscoveryProbeResponseDto(
    String mode,
    String serviceKey,
    String serviceId,
    String instanceId,
    String instanceUri,
    int httpStatus,
    boolean healthy,
    long durationMillis) {}
