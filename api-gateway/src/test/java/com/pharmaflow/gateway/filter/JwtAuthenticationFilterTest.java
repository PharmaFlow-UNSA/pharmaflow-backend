package com.pharmaflow.gateway.filter;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthenticationFilterTest {

    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(null, null, null);

    @Test
    void hasAccessToPath_allowsUserToReadPrescriptions() {
        boolean hasAccess = filter.hasAccessToPath(
                "/api/prescriptions",
                HttpMethod.GET,
                List.of("ROLE_USER"));

        assertThat(hasAccess).isTrue();
    }

    @Test
    void hasAccessToPath_allowsUserToUploadPrescription() {
        boolean hasAccess = filter.hasAccessToPath(
                "/api/prescriptions",
                HttpMethod.POST,
                List.of("ROLE_USER"));

        assertThat(hasAccess).isTrue();
    }

    @Test
    void hasAccessToPath_blocksUserFromReviewingPrescription() {
        boolean hasAccess = filter.hasAccessToPath(
                "/api/prescriptions/1",
                HttpMethod.PATCH,
                List.of("ROLE_USER"));

        assertThat(hasAccess).isFalse();
    }

    @Test
    void hasAccessToPath_allowsPharmacistToReviewPrescription() {
        boolean hasAccess = filter.hasAccessToPath(
                "/api/prescriptions/1",
                HttpMethod.PATCH,
                List.of("ROLE_PHARMACIST"));

        assertThat(hasAccess).isTrue();
    }

    @Test
    void isPublicCatalogRead_allowsSafeCatalogPaths() {
        assertThat(filter.isPublicCatalogRead("/api/products/page")).isTrue();
        assertThat(filter.isPublicCatalogRead("/api/categories")).isTrue();
        assertThat(filter.isPublicCatalogRead("/api/pharmacies/1")).isTrue();
        assertThat(filter.isPublicCatalogRead("/api/inventory/product/1")).isTrue();
        assertThat(filter.isPublicCatalogRead("/api/inventory/product-summary")).isTrue();
    }

    @Test
    void isPublicCatalogRead_doesNotAllowGeneralInventoryReads() {
        assertThat(filter.isPublicCatalogRead("/api/inventory")).isFalse();
        assertThat(filter.isPublicCatalogRead("/api/inventory/1")).isFalse();
    }
}
