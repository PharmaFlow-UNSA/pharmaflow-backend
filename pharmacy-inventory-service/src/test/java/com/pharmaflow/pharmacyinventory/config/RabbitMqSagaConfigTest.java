package com.pharmaflow.pharmacyinventory.config;

import com.pharmaflow.pharmacyinventory.config.RabbitMqSagaConfig.SagaRabbitProperties;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;

import static org.assertj.core.api.Assertions.assertThat;

class RabbitMqSagaConfigTest {

    private final RabbitMqSagaConfig config = new RabbitMqSagaConfig();
    private final SagaRabbitProperties properties = new SagaRabbitProperties();

    @Test
    void sagaExchangeShouldBeDurableDirectExchange() {
        DirectExchange exchange = config.sagaExchange(properties);
        assertThat(exchange.getName()).isEqualTo("pharmaflow.saga.exchange");
        assertThat(exchange.isDurable()).isTrue();
        assertThat(exchange.isAutoDelete()).isFalse();
    }

    @Test
    void orderFulfillmentQueuesShouldBeDurable() {
        Queue placed = config.orderPlacedQueue(properties);
        Queue cancelled = config.orderCancelledQueue(properties);

        assertThat(placed.getName()).isEqualTo("order.placed.queue");
        assertThat(cancelled.getName()).isEqualTo("order.cancelled.queue");
        assertThat(placed.isDurable()).isTrue();
        assertThat(cancelled.isDurable()).isTrue();
    }

    @Test
    void orderFulfillmentBindingsShouldUseConfiguredRoutingKeys() {
        DirectExchange exchange = config.sagaExchange(properties);

        Binding placedBinding = config.orderPlacedBinding(
                properties, exchange, config.orderPlacedQueue(properties));
        Binding cancelledBinding = config.orderCancelledBinding(
                properties, exchange, config.orderCancelledQueue(properties));

        assertThat(placedBinding.getRoutingKey()).isEqualTo("order.placed");
        assertThat(placedBinding.getDestination()).isEqualTo("order.placed.queue");
        assertThat(cancelledBinding.getRoutingKey()).isEqualTo("order.cancelled");
        assertThat(cancelledBinding.getDestination()).isEqualTo("order.cancelled.queue");
    }

    @Test
    void existingRecommendationSagaPropertiesShouldStillDefault() {
        // Defensive: make sure we did not accidentally rename the recommendation-saga keys
        // while extending the property class for Zadatak 8.3.
        assertThat(properties.getRecommendationReservationRequestedRoutingKey())
                .isEqualTo("recommendation.reservation.requested");
        assertThat(properties.getReservationCreatedRoutingKey()).isEqualTo("reservation.created");
        assertThat(properties.getReservationRejectedRoutingKey()).isEqualTo("reservation.rejected");
    }

    @Test
    void orderFulfillmentPropertiesShouldHaveExpectedDefaults() {
        assertThat(properties.getOrderPlacedRoutingKey()).isEqualTo("order.placed");
        assertThat(properties.getOrderCancelledRoutingKey()).isEqualTo("order.cancelled");
        assertThat(properties.getOrderStockReservedRoutingKey()).isEqualTo("order.stock.reserved");
        assertThat(properties.getOrderStockRejectedRoutingKey()).isEqualTo("order.stock.rejected");
        assertThat(properties.getOrderStockRestockedRoutingKey()).isEqualTo("order.stock.restocked");
    }
}
