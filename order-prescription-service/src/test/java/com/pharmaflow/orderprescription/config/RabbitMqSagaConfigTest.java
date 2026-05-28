package com.pharmaflow.orderprescription.config;

import com.pharmaflow.orderprescription.config.RabbitMqSagaConfig.OrderSagaRabbitProperties;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;

import static org.assertj.core.api.Assertions.assertThat;

class RabbitMqSagaConfigTest {

    private final RabbitMqSagaConfig config = new RabbitMqSagaConfig();
    private final OrderSagaRabbitProperties properties = new OrderSagaRabbitProperties();

    @Test
    void sagaExchangeShouldBeDurableDirectExchange() {
        DirectExchange exchange = config.sagaExchange(properties);
        assertThat(exchange.getName()).isEqualTo("pharmaflow.saga.exchange");
        assertThat(exchange.isDurable()).isTrue();
        assertThat(exchange.isAutoDelete()).isFalse();
    }

    @Test
    void orderStockQueuesShouldBeDurable() {
        Queue reserved = config.orderStockReservedQueue(properties);
        Queue rejected = config.orderStockRejectedQueue(properties);
        Queue restocked = config.orderStockRestockedQueue(properties);

        assertThat(reserved.getName()).isEqualTo("order.stock.reserved.queue");
        assertThat(rejected.getName()).isEqualTo("order.stock.rejected.queue");
        assertThat(restocked.getName()).isEqualTo("order.stock.restocked.queue");
        assertThat(reserved.isDurable()).isTrue();
        assertThat(rejected.isDurable()).isTrue();
        assertThat(restocked.isDurable()).isTrue();
    }

    @Test
    void orderStockBindingsShouldUseConfiguredRoutingKeys() {
        DirectExchange exchange = config.sagaExchange(properties);

        Binding reserved = config.orderStockReservedBinding(
                properties, exchange, config.orderStockReservedQueue(properties));
        Binding rejected = config.orderStockRejectedBinding(
                properties, exchange, config.orderStockRejectedQueue(properties));
        Binding restocked = config.orderStockRestockedBinding(
                properties, exchange, config.orderStockRestockedQueue(properties));

        assertThat(reserved.getRoutingKey()).isEqualTo("order.stock.reserved");
        assertThat(rejected.getRoutingKey()).isEqualTo("order.stock.rejected");
        assertThat(restocked.getRoutingKey()).isEqualTo("order.stock.restocked");
        assertThat(reserved.getDestination()).isEqualTo("order.stock.reserved.queue");
    }

    @Test
    void propertyDefaultsShouldMatchExpectedRoutingKeys() {
        assertThat(properties.getOrderPlacedRoutingKey()).isEqualTo("order.placed");
        assertThat(properties.getOrderCancelledRoutingKey()).isEqualTo("order.cancelled");
        assertThat(properties.getOrderStockReservedRoutingKey()).isEqualTo("order.stock.reserved");
        assertThat(properties.getOrderStockRejectedRoutingKey()).isEqualTo("order.stock.rejected");
        assertThat(properties.getOrderStockRestockedRoutingKey()).isEqualTo("order.stock.restocked");
    }
}
