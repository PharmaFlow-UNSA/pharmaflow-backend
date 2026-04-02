package com.pharmaflow.orderprescription.config;

import com.pharmaflow.orderprescription.models.*;
import com.pharmaflow.orderprescription.repositories.AutoRefillSubscriptionRepository;
import com.pharmaflow.orderprescription.repositories.OrderRepository;
import com.pharmaflow.orderprescription.repositories.PrescriptionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    private final OrderRepository orderRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final AutoRefillSubscriptionRepository autoRefillSubscriptionRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public DataSeeder(OrderRepository orderRepository,
                      PrescriptionRepository prescriptionRepository,
                      AutoRefillSubscriptionRepository autoRefillSubscriptionRepository) {
        this.orderRepository = orderRepository;
        this.prescriptionRepository = prescriptionRepository;
        this.autoRefillSubscriptionRepository = autoRefillSubscriptionRepository;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        resetDatabase();
        seedData();
    }

    private void resetDatabase() {
        entityManager.createNativeQuery("TRUNCATE TABLE order_items, orders, payments, auto_refill_subscriptions, prescriptions RESTART IDENTITY CASCADE").executeUpdate();
        System.out.println(">>> Baza ociscena i ID-ovi resetovani na 1.");
    }

    private void seedData() {
        String[] prescriptionStatuses = {"PENDING", "APPROVED", "REJECTED", "APPROVED", "PENDING"};
        String[] orderStatuses = {"PENDING", "CONFIRMED", "SHIPPED", "DELIVERED", "CANCELLED"};
        String[] paymentMethods = {"CARD", "CASH", "TRANSFER", "CARD", "CASH"};
        String[] productNames = {"Brufen 400mg", "Paracetamol 500mg", "Amoksicilin 500mg",
                "Aspirin 100mg", "Ibuprofen 200mg", "Vitamin C 1000mg",
                "Pantoprazol 20mg", "Metformin 500mg", "Losartan 50mg", "Atorvastatin 20mg"};
        String[] cities = {"Sarajevo", "Mostar", "Tuzla", "Banja Luka", "Zenica"};

        // Seed prescriptions
        List<Prescription> prescriptions = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Prescription p = new Prescription();
            p.setUserId((long) i);
            p.setImageUrl("/uploads/recept_user" + i + ".pdf");
            p.setStatus(prescriptionStatuses[i - 1]);
            p.setUploadedAt(LocalDateTime.now().minusDays(i));
            if (!p.getStatus().equals("PENDING")) {
                p.setReviewedAt(LocalDateTime.now().minusDays(i).plusHours(4));
                p.setReviewerNotes(p.getStatus().equals("APPROVED") ? "Recept validan." : "Recept nejasan, potrebna ponovna dostava.");
            }
            prescriptions.add(p);
        }
        prescriptionRepository.saveAll(prescriptions);

        // Seed orders with items and payments
        List<Order> orders = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Order order = new Order();
            order.setUserId((long) i);
            order.setStatus(orderStatuses[i - 1]);
            order.setShippingAddress("Ulica_" + i + " bb, " + cities[i - 1]);
            order.setCreatedAt(LocalDateTime.now().minusDays(i));
            if (!order.getStatus().equals("PENDING")) {
                order.setUpdatedAt(LocalDateTime.now().minusDays(i).plusHours(2));
            }

            BigDecimal total = BigDecimal.ZERO;
            for (int j = 1; j <= 2; j++) {
                OrderItem item = new OrderItem();
                int productIndex = (i - 1) * 2 + (j - 1);
                item.setProductId((long) (productIndex + 1));
                item.setProductName(productNames[productIndex]);
                item.setQuantity(j);
                BigDecimal price = BigDecimal.valueOf(5.0 + (i * 2) + j);
                item.setUnitPrice(price);
                item.setOrder(order);
                order.getOrderItems().add(item);
                total = total.add(price.multiply(BigDecimal.valueOf(j)));
            }
            order.setTotalAmount(total);

            Payment payment = new Payment();
            payment.setAmount(total);
            payment.setMethod(paymentMethods[i - 1]);
            payment.setStatus(order.getStatus().equals("DELIVERED") ? "COMPLETED" : "PENDING");
            payment.setTransactionId("TXN-" + String.format("%05d", i));
            if (payment.getStatus().equals("COMPLETED")) {
                payment.setPaidAt(LocalDateTime.now().minusDays(i).plusHours(1));
            }
            order.setPayment(payment);

            // Link approved prescriptions to orders
            if (i <= prescriptions.size() && prescriptions.get(i - 1).getStatus().equals("APPROVED")) {
                order.setPrescription(prescriptions.get(i - 1));
            }

            orders.add(order);
        }
        orderRepository.saveAll(orders);

        // Seed auto-refill subscriptions
        List<AutoRefillSubscription> subscriptions = new ArrayList<>();
        String[] subStatuses = {"ACTIVE", "ACTIVE", "PAUSED", "ACTIVE", "CANCELLED"};
        for (int i = 1; i <= 5; i++) {
            AutoRefillSubscription sub = new AutoRefillSubscription();
            sub.setUserId((long) i);
            sub.setProductId((long) (i * 2));
            sub.setDosagePerDay(i % 2 == 0 ? 2 : 1);
            sub.setTabletsPerPackage(30);
            sub.setIntervalDays(sub.getTabletsPerPackage() / sub.getDosagePerDay());
            sub.setNextOrderDate(LocalDate.now().plusDays(sub.getIntervalDays() - 5));
            sub.setStatus(subStatuses[i - 1]);
            sub.setShippingAddress("Ulica_" + i + " bb, " + cities[i - 1]);

            // Link approved prescriptions to subscriptions
            if (i <= prescriptions.size() && prescriptions.get(i - 1).getStatus().equals("APPROVED")) {
                sub.setPrescription(prescriptions.get(i - 1));
            }

            subscriptions.add(sub);
        }
        autoRefillSubscriptionRepository.saveAll(subscriptions);

        System.out.println(">>> Baza uspjesno popunjena (5 recepata, 5 narudzbi sa stavkama i placanjima, 5 pretplata).");
    }
}
