package com.pharmaflow.pharmacyinventory.config;

import com.pharmaflow.pharmacyinventory.models.*;
import com.pharmaflow.pharmacyinventory.repositories.PharmacyRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class DataSeeder implements CommandLineRunner {

    private final PharmacyRepository pharmacyRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public DataSeeder(PharmacyRepository pharmacyRepository) {
        this.pharmacyRepository = pharmacyRepository;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        resetDatabase();
        seedData();
    }

    private void resetDatabase() {
        pharmacyRepository.deleteAll();
        entityManager.createNativeQuery("TRUNCATE TABLE pharmacies RESTART IDENTITY CASCADE").executeUpdate();
        entityManager.createNativeQuery("TRUNCATE TABLE inventories RESTART IDENTITY CASCADE").executeUpdate();
        entityManager.createNativeQuery("TRUNCATE TABLE reservations RESTART IDENTITY CASCADE").executeUpdate();
        entityManager.createNativeQuery("TRUNCATE TABLE deliveries RESTART IDENTITY CASCADE").executeUpdate();
        System.out.println(">>> Baza ociscena i ID-ovi resetovani na 1.");
    }

    private void seedData() {
        String[] cities = {"Sarajevo", "Mostar", "Tuzla", "Banja Luka", "Zenica"};
        String[] deliveryStatuses = {"PREPARING", "IN_TRANSIT", "DELIVERED"};
        List<Pharmacy> pharmacies = new ArrayList<>();

        for (int i = 1; i <= 5; i++) {
            Pharmacy pharmacy = new Pharmacy();
            pharmacy.setName("Apoteka_" + i);
            pharmacy.setAddress("Ulica_" + i + " bb");
            pharmacy.setCity(cities[i - 1]);
            pharmacy.setPhoneNumber("+387-33-" + (100000 + i));
            pharmacy.setEmail("apoteka" + i + "@pharmaflow.ba");
            pharmacy.setOpeningHours("08:00-20:00");

            for (int j = 1; j <= 2; j++) {
                Inventory inv = new Inventory();
                inv.setProductId((long) ((i - 1) * 2 + j));
                inv.setQuantity(50 + (i * 10));
                inv.setReorderLevel(10);
                inv.setLastRestocked(LocalDate.now().minusDays(i));
                inv.setPharmacy(pharmacy);
                pharmacy.getInventoryItems().add(inv);
            }

            for (int j = 1; j <= 2; j++) {
                Reservation res = new Reservation();
                res.setUserId((long) i);
                res.setProductId((long) j);
                res.setQuantity(1);
                res.setStatus(j % 2 == 0 ? "READY" : "PENDING");
                res.setReservedAt(LocalDateTime.now().minusHours(i));
                res.setExpiresAt(LocalDateTime.now().plusDays(1));
                res.setPharmacy(pharmacy);
                pharmacy.getReservations().add(res);
            }

            Delivery del = new Delivery();
            del.setOrderId((long) i);
            del.setDeliveryAddress("Adresa_" + i + ", " + cities[i - 1]);
            del.setStatus(deliveryStatuses[(i - 1) % 3]);
            del.setEstimatedDelivery(LocalDateTime.now().plusDays(i));
            if (del.getStatus().equals("DELIVERED")) {
                del.setActualDelivery(LocalDateTime.now().minusHours(2));
            }
            del.setPharmacy(pharmacy);
            pharmacy.getDeliveries().add(del);

            pharmacies.add(pharmacy);
        }

        pharmacyRepository.saveAll(pharmacies);
        System.out.println(">>> Baza uspjesno popunjena (5 apoteka sa inventarom, rezervacijama i dostavama).");
    }
}
