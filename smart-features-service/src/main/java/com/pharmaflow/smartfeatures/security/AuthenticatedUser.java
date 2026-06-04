package com.pharmaflow.smartfeatures.security;

public record AuthenticatedUser(String subject, Long userId) {}
