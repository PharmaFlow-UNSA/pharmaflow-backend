package com.pharmaflow.pharmacyinventory.config;

import com.pharmaflow.pharmacyinventory.models.*;
import com.pharmaflow.pharmacyinventory.repositories.PharmacyRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
@Profile("!test")
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
        String[] names = {
                "PharmaFlow Centar",
                "Apoteka Stari Grad",
                "Moja Apoteka Mostar",
                "Tuzla Health Pharmacy",
                "Banja Luka Care",
                "Zenica Medica",
                "Bihac Family Pharmacy",
                "Doboj Wellness Point"
        };
        String[] addresses = {
                "Marsala Tita 21",
                "Zelenih beretki 8",
                "Kralja Tvrtka 12",
                "Trg Slobode 4",
                "Kralja Petra I Karadordevica 97",
                "Bulevar Kulina bana 15",
                "Bosanska 44",
                "Svetog Save 18"
        };
        String[] cities = {"Sarajevo", "Sarajevo", "Mostar", "Tuzla", "Banja Luka", "Zenica", "Bihac", "Doboj"};
        String[] imageUrls = {
                "/demo/pharmacies/pharmaflow-centar.png",
                "/demo/pharmacies/apoteka-stari-grad.png",
                "/demo/pharmacies/moja-apoteka-mostar.png",
                "/demo/pharmacies/tuzla-health-pharmacy.jpg",
                "/demo/pharmacies/banja-luka-care.png",
                "/demo/pharmacies/zenica-medica.png",
                "/demo/pharmacies/bihac-family-pharmacy.png",
                "/demo/pharmacies/doboj-wellness-point.png"
        };
        String[] deliveryStatuses = {"PREPARING", "IN_TRANSIT", "DELIVERED", "RETURNED"};
        List<Pharmacy> pharmacies = new ArrayList<>();

        for (int i = 1; i <= names.length; i++) {
            Pharmacy pharmacy = new Pharmacy();
            pharmacy.setName(names[i - 1]);
            pharmacy.setAddress(addresses[i - 1]);
            pharmacy.setCity(cities[i - 1]);
            pharmacy.setPhoneNumber("+387-33-" + (100000 + i));
            pharmacy.setEmail("pharmacy" + i + "@pharmaflow.ba");
            pharmacy.setOpeningHours(i == 1 ? "08:00-22:00" : "08:00-20:00");
            pharmacy.setImageUrl(imageUrls[i - 1]);

            for (int j = 0; j < 45; j++) {
                long productId = ((i - 1L) * 11 + j * 2L) % 100L + 1L;
                Inventory inv = new Inventory();
                inv.setProductId(productId);
                inv.setQuantity((int) ((productId * 7 + i * 5) % 34) + 2);
                inv.setReorderLevel(productId <= 20 ? 12 : 8);
                inv.setLastRestocked(LocalDate.now().minusDays((i + j) % 14));
                inv.setPharmacy(pharmacy);
                pharmacy.getInventoryItems().add(inv);
            }

            for (int j = 1; j <= 4; j++) {
                Reservation res = new Reservation();
                res.setUserId((long) ((i + j) % 6 + 1));
                res.setProductId(((i - 1L) * 13 + j * 5L) % 100L + 1L);
                res.setQuantity(j % 3 + 1);
                res.setStatus(j % 3 == 0 ? "READY" : j % 2 == 0 ? "CONFIRMED" : "PENDING");
                res.setReservedAt(LocalDateTime.now().minusHours(i + j));
                res.setExpiresAt(LocalDateTime.now().plusDays(j));
                res.setPharmacy(pharmacy);
                pharmacy.getReservations().add(res);
            }

            for (int j = 1; j <= 2; j++) {
                Delivery del = new Delivery();
                del.setOrderId((long) ((i - 1) * 2 + j));
                del.setDeliveryAddress("Demo address " + j + ", " + cities[i - 1]);
                del.setStatus(deliveryStatuses[(i + j - 2) % deliveryStatuses.length]);
                del.setEstimatedDelivery(LocalDateTime.now().plusDays(j));
                if (del.getStatus().equals("DELIVERED")) {
                    del.setActualDelivery(LocalDateTime.now().minusHours(2));
                }
                del.setPharmacy(pharmacy);
                pharmacy.getDeliveries().add(del);
            }

            pharmacies.add(pharmacy);
        }

        pharmacyRepository.saveAll(pharmacies);
        System.out.println(">>> Baza uspjesno popunjena (8 apoteka sa sirokim inventarom, rezervacijama i dostavama).");
    }
}
