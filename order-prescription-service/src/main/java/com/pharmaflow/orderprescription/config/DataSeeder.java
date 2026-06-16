package com.pharmaflow.orderprescription.config;

import com.pharmaflow.orderprescription.models.*;
import com.pharmaflow.orderprescription.repositories.AutoRefillSubscriptionRepository;
import com.pharmaflow.orderprescription.repositories.OrderRepository;
import com.pharmaflow.orderprescription.repositories.PrescriptionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@Profile("!test")
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
        String[] prescriptionStatuses = {"PENDING", "APPROVED", "REJECTED", "APPROVED", "PENDING", "APPROVED", "PENDING", "APPROVED"};
        String[] orderStatuses = {"PENDING", "CONFIRMED", "SHIPPED", "DELIVERED", "CANCELLED"};
        String[] paymentMethods = {"CARD", "CASH", "TRANSFER", "CARD", "CASH"};
        String[] productNames = demoProductNames();
        String[] cities = {"Sarajevo", "Mostar", "Tuzla", "Banja Luka", "Zenica", "Bihac", "Doboj", "Travnik"};

        // Seed prescriptions
        List<Prescription> prescriptions = new ArrayList<>();
        for (int i = 1; i <= prescriptionStatuses.length; i++) {
            Prescription p = new Prescription();
            p.setUserId((long) ((i - 1) % 6 + 1));
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
        for (int i = 1; i <= 20; i++) {
            Order order = new Order();
            order.setUserId((long) ((i - 1) % 6 + 1));
            order.setStatus(orderStatuses[(i - 1) % orderStatuses.length]);
            order.setShippingAddress("Demo street " + i + " bb, " + cities[(i - 1) % cities.length]);
            order.setCreatedAt(LocalDateTime.now().minusDays(i));
            if (!order.getStatus().equals("PENDING")) {
                order.setUpdatedAt(LocalDateTime.now().minusDays(i).plusHours(2));
            }

            BigDecimal total = BigDecimal.ZERO;
            for (int j = 1; j <= 3; j++) {
                OrderItem item = new OrderItem();
                int productIndex = ((i - 1) * 5 + (j - 1) * 7) % productNames.length;
                item.setProductId((long) productIndex + 1);
                item.setProductName(productNames[productIndex]);
                item.setQuantity((i + j) % 3 + 1);
                BigDecimal price = BigDecimal.valueOf(3.5 + ((productIndex * 1.35) % 22.0)).setScale(2, java.math.RoundingMode.HALF_UP);
                item.setUnitPrice(price);
                item.setOrder(order);
                order.getOrderItems().add(item);
                total = total.add(price.multiply(BigDecimal.valueOf(item.getQuantity())));
            }
            order.setTotalAmount(total);

            Payment payment = new Payment();
            payment.setAmount(total);
            payment.setMethod(paymentMethods[(i - 1) % paymentMethods.length]);
            payment.setStatus(order.getStatus().equals("DELIVERED") ? "COMPLETED" : "PENDING");
            payment.setTransactionId("TXN-" + String.format("%05d", i));
            if (payment.getStatus().equals("COMPLETED")) {
                payment.setPaidAt(LocalDateTime.now().minusDays(i).plusHours(1));
            }
            order.setPayment(payment);

            // Link approved prescriptions to orders
            Prescription prescription = prescriptions.get((i - 1) % prescriptions.size());
            if (prescription.getStatus().equals("APPROVED")) {
                order.setPrescription(prescription);
            }

            orders.add(order);
        }
        orderRepository.saveAll(orders);

        // Seed auto-refill subscriptions
        List<AutoRefillSubscription> subscriptions = new ArrayList<>();
        String[] subStatuses = {"ACTIVE", "ACTIVE", "PAUSED", "ACTIVE", "CANCELLED", "ACTIVE", "PAUSED", "ACTIVE"};
        for (int i = 1; i <= subStatuses.length; i++) {
            AutoRefillSubscription sub = new AutoRefillSubscription();
            sub.setUserId((long) ((i - 1) % 6 + 1));
            sub.setProductId((long) (60 + i));
            sub.setDosagePerDay(i % 2 == 0 ? 2 : 1);
            sub.setTabletsPerPackage(30);
            sub.setIntervalDays(sub.getTabletsPerPackage() / sub.getDosagePerDay());
            sub.setNextOrderDate(LocalDate.now().plusDays(sub.getIntervalDays() - 5));
            sub.setStatus(subStatuses[i - 1]);
            sub.setShippingAddress("Demo street " + i + " bb, " + cities[(i - 1) % cities.length]);

            // Link approved prescriptions to subscriptions
            Prescription prescription = prescriptions.get((i - 1) % prescriptions.size());
            if (prescription.getStatus().equals("APPROVED")) {
                sub.setPrescription(prescription);
            }

            subscriptions.add(sub);
        }
        autoRefillSubscriptionRepository.saveAll(subscriptions);

        System.out.println(">>> Baza uspjesno popunjena (8 recepata, 20 narudzbi sa stavkama i placanjima, 8 pretplata).");
    }

    private String[] demoProductNames() {
        return new String[]{
                "Brufen 400mg tablets", "Ibuprofen 400mg tablets", "Panadol 500mg tablets", "Paracetamol 500mg tablets", "Aspirin Protect 100mg tablets",
                "Nalgesin S 220mg tablets", "Daleron C granules", "Nurofen Express 200mg capsules", "Voltaren Emulgel 1% gel", "Deep Relief Ibuprofen gel",
                "Amoxicillin 500mg capsules", "Azithromycin 500mg tablets", "Cefalexin 500mg capsules", "Doxycycline 100mg capsules", "Ciprofloxacin 500mg tablets",
                "Clarithromycin 500mg tablets", "Metronidazole 400mg tablets", "Nitrofurantoin 100mg capsules", "Fusidic Acid 2% cream", "Mupirocin 2% ointment",
                "Omeprazole 20mg capsules", "Pantoprazole 40mg tablets", "Controloc 20mg tablets", "Loperamide 2mg capsules", "Smecta sachets",
                "Probiotic Complex capsules", "Buscopan 10mg tablets", "Rennie chewable tablets", "Gaviscon Advance liquid", "Lactulose syrup 200ml",
                "Vitamin C 1000mg effervescent tablets", "Vitamin D3 2000 IU capsules", "Magnesium 375mg tablets", "Centrum multivitamin tablets", "Omega-3 1000mg softgels",
                "Zinc 15mg tablets", "B-Complex forte tablets", "Iron Plus capsules", "Calcium D3 chewables", "Electrolyte rehydration sachets",
                "Cetirizine 10mg tablets", "Loratadine 10mg tablets", "Aerius 5mg tablets", "Flixonase nasal spray", "Aqua Maris nasal spray",
                "Allergy Eye Drops", "Pseudoephedrine 60mg tablets", "Xylometazoline nasal spray", "Saline nasal rinse kit", "Sinus Relief capsules",
                "Dry Cough Syrup 150ml", "Ivy Leaf Cough Syrup 100ml", "Strepsils Honey Lemon lozenges", "Isla-Mint throat pastilles", "ACC 600mg effervescent tablets",
                "Bronchostop syrup", "Vicks VapoRub 50g", "Theraflu powder sachets", "Olynth nasal spray", "Sore Throat Spray 30ml",
                "Warfarin 5mg tablets", "Amlodipine 5mg tablets", "Losartan 50mg tablets", "Atorvastatin 20mg tablets", "Bisoprolol 5mg tablets",
                "Ramipril 5mg capsules", "Aspirin Cardio 100mg tablets", "Furosemide 40mg tablets", "Rosuvastatin 10mg tablets", "Nitroglycerin spray",
                "Hydrocortisone 1% cream", "Bepanthen ointment", "Antiseptic Spray 100ml", "Betadine solution 100ml", "Sterile Gauze Pads",
                "Elastic Bandage 8cm", "Medical Plasters assorted", "Burn Gel 50ml", "Wound Cleansing Wipes", "Clotrimazole 1% cream",
                "Digital Thermometer", "Blood Pressure Monitor", "Pulse Oximeter", "Glucose Meter Starter Kit", "Lancets 100 pack",
                "Test Strips 50 pack", "Nebulizer Compressor", "Heating Pad", "Pill Organizer weekly", "Pregnancy Test twin pack",
                "La Roche-Posay Cicaplast Balm", "Eucerin UreaRepair Lotion", "Bioderma Atoderm Shower Gel", "Avene Thermal Water Spray", "Sunscreen SPF50 Sensitive",
                "Baby Diaper Cream", "Lip Balm SPF30", "Hand Sanitizer Gel 500ml", "Micellar Cleansing Water", "Anti-Dandruff Shampoo"
        };
    }
}
