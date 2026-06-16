package com.pharmaflow.producthealth.config;

import com.pharmaflow.producthealth.models.Category;
import com.pharmaflow.producthealth.models.Contraindication;
import com.pharmaflow.producthealth.models.DrugInteraction;
import com.pharmaflow.producthealth.models.Product;
import com.pharmaflow.producthealth.models.ProductSubstitute;
import com.pharmaflow.producthealth.models.Substance;
import com.pharmaflow.producthealth.repositories.CategoryRepository;
import com.pharmaflow.producthealth.repositories.ContraindicationRepository;
import com.pharmaflow.producthealth.repositories.DrugInteractionRepository;
import com.pharmaflow.producthealth.repositories.ProductRepository;
import com.pharmaflow.producthealth.repositories.ProductSubstituteRepository;
import com.pharmaflow.producthealth.repositories.SubstanceRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@Profile("!test")
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private static final String DEMO_IMAGE_PATH = "/demo/products/";

    private final CategoryRepository categoryRepository;
    private final SubstanceRepository substanceRepository;
    private final ProductRepository productRepository;
    private final DrugInteractionRepository drugInteractionRepository;
    private final ContraindicationRepository contraindicationRepository;
    private final ProductSubstituteRepository productSubstituteRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public void run(String... args) {
        System.out.println("=== PharmaFlow: Rebuilding generated product-health demo data ===");
        resetDatabase();
        seedData();
    }

    private void resetDatabase() {
        productSubstituteRepository.deleteAll();
        drugInteractionRepository.deleteAll();
        contraindicationRepository.deleteAll();
        productRepository.deleteAll();
        substanceRepository.deleteAll();
        categoryRepository.deleteAll();
        entityManager.createNativeQuery("TRUNCATE TABLE product_substitutes, product_substances, drug_interactions, contraindications, products, substances, categories RESTART IDENTITY CASCADE")
                .executeUpdate();
    }

    private void seedData() {
        Map<String, Category> categories = seedCategories();
        Map<String, Substance> substances = seedSubstances();
        List<Product> products = seedProducts(categories, substances);
        seedInteractionsAndContraindications(substances);
        seedSubstitutes(products);

        System.out.println("=== Generated product-health data loaded successfully ===");
        System.out.println("    Categories  : " + categoryRepository.count());
        System.out.println("    Substances  : " + substanceRepository.count());
        System.out.println("    Products    : " + productRepository.count());
        System.out.println("    Interactions: " + drugInteractionRepository.count());
        System.out.println("    Contraind.  : " + contraindicationRepository.count());
        System.out.println("    Substitutes : " + productSubstituteRepository.count());
    }

    private Map<String, Category> seedCategories() {
        Map<String, String> descriptions = new LinkedHashMap<>();
        descriptions.put("Analgesics and Antipyretics", "Pain relief, fever reduction and anti-inflammatory products");
        descriptions.put("Antibiotics", "Prescription antibacterial medicines and topical antibiotics");
        descriptions.put("Gastrointestinal Medications", "Digestive health, reflux, diarrhea, constipation and probiotic care");
        descriptions.put("Vitamins and Supplements", "Vitamins, minerals, omega oils and daily wellness supplements");
        descriptions.put("Allergy Relief", "Antihistamines, nasal sprays and allergy comfort products");
        descriptions.put("Cold, Flu and Respiratory", "Cough, sore throat, congestion and respiratory relief");
        descriptions.put("Cardiovascular Care", "Prescription and supportive products for heart and circulation care");
        descriptions.put("Dermatology and First Aid", "Skin care, wound care and first-aid essentials");
        descriptions.put("Medical Devices", "Home care, monitoring and diagnostic devices");
        descriptions.put("Dermocosmetics", "Pharmacy skin care, sun care and personal hygiene products");

        Map<String, Category> categories = new LinkedHashMap<>();
        descriptions.forEach((name, description) -> {
            Category category = new Category();
            category.setName(name);
            category.setDescription(description);
            categories.put(name, categoryRepository.save(category));
        });
        return categories;
    }

    private Map<String, Substance> seedSubstances() {
        List<SubstanceSeed> seeds = List.of(
                new SubstanceSeed("ibuprofen", "Ibuprofen", "M01AE01", "NSAID analgesic, antipyretic and anti-inflammatory."),
                new SubstanceSeed("paracetamol", "Paracetamol (Acetaminophen)", "N02BE01", "Analgesic and antipyretic for pain and fever."),
                new SubstanceSeed("acetylsalicylic acid", "Acetylsalicylic acid", "B01AC06", "Antiplatelet and analgesic medicine."),
                new SubstanceSeed("naproxen", "Naproxen", "M01AE02", "NSAID for pain and inflammation."),
                new SubstanceSeed("diclofenac", "Diclofenac", "M01AB05", "NSAID for musculoskeletal pain and inflammation."),
                new SubstanceSeed("amoxicillin", "Amoxicillin", "J01CA04", "Broad-spectrum penicillin antibiotic."),
                new SubstanceSeed("azithromycin", "Azithromycin", "J01FA10", "Macrolide antibiotic."),
                new SubstanceSeed("cefalexin", "Cefalexin", "J01DB01", "First-generation cephalosporin antibiotic."),
                new SubstanceSeed("doxycycline", "Doxycycline", "J01AA02", "Tetracycline antibiotic."),
                new SubstanceSeed("ciprofloxacin", "Ciprofloxacin", "J01MA02", "Fluoroquinolone antibiotic."),
                new SubstanceSeed("clarithromycin", "Clarithromycin", "J01FA09", "Macrolide antibiotic."),
                new SubstanceSeed("metronidazole", "Metronidazole", "J01XD01", "Antibacterial and antiprotozoal medicine."),
                new SubstanceSeed("nitrofurantoin", "Nitrofurantoin", "J01XE01", "Urinary antiseptic antibiotic."),
                new SubstanceSeed("omeprazole", "Omeprazole", "A02BC01", "Proton pump inhibitor."),
                new SubstanceSeed("pantoprazole", "Pantoprazole", "A02BC02", "Proton pump inhibitor."),
                new SubstanceSeed("loperamide", "Loperamide", "A07DA03", "Antidiarrheal medicine."),
                new SubstanceSeed("probiotic blend", "Probiotic blend", "A07FA", "Live cultures for digestive flora support."),
                new SubstanceSeed("ascorbic acid", "Vitamin C", "A11GA01", "Vitamin C immune and antioxidant support."),
                new SubstanceSeed("colecalciferol", "Vitamin D3", "A11CC05", "Vitamin D3 bone and immune support."),
                new SubstanceSeed("magnesium", "Magnesium", "A12CC", "Mineral for muscle and nervous system function."),
                new SubstanceSeed("omega-3-acid ethyl esters", "Omega-3", "C10AX06", "Omega fatty acids for nutrition support."),
                new SubstanceSeed("zinc", "Zinc", "A12CB", "Trace mineral for immune support."),
                new SubstanceSeed("cetirizine", "Cetirizine", "R06AE07", "Second-generation antihistamine."),
                new SubstanceSeed("loratadine", "Loratadine", "R06AX13", "Non-sedating antihistamine."),
                new SubstanceSeed("desloratadine", "Desloratadine", "R06AX27", "Non-sedating antihistamine."),
                new SubstanceSeed("fluticasone", "Fluticasone", "R01AD08", "Corticosteroid nasal spray."),
                new SubstanceSeed("pseudoephedrine", "Pseudoephedrine", "R01BA02", "Nasal decongestant."),
                new SubstanceSeed("xylometazoline", "Xylometazoline", "R01AA07", "Topical nasal decongestant."),
                new SubstanceSeed("dextromethorphan", "Dextromethorphan", "R05DA09", "Cough suppressant."),
                new SubstanceSeed("acetylcysteine", "Acetylcysteine", "R05CB01", "Mucolytic for productive cough."),
                new SubstanceSeed("warfarin", "Warfarin", "B01AA03", "Anticoagulant medicine."),
                new SubstanceSeed("amlodipine", "Amlodipine", "C08CA01", "Calcium channel blocker."),
                new SubstanceSeed("losartan", "Losartan", "C09CA01", "Angiotensin receptor blocker."),
                new SubstanceSeed("atorvastatin", "Atorvastatin", "C10AA05", "Statin lipid-lowering medicine."),
                new SubstanceSeed("bisoprolol", "Bisoprolol", "C07AB07", "Beta blocker."),
                new SubstanceSeed("ramipril", "Ramipril", "C09AA05", "ACE inhibitor."),
                new SubstanceSeed("furosemide", "Furosemide", "C03CA01", "Loop diuretic."),
                new SubstanceSeed("hydrocortisone", "Hydrocortisone", "D07AA02", "Mild topical corticosteroid."),
                new SubstanceSeed("povidone iodine", "Povidone-iodine", "D08AG02", "Topical antiseptic."),
                new SubstanceSeed("clotrimazole", "Clotrimazole", "D01AC01", "Topical antifungal."),
                new SubstanceSeed("dexpanthenol", "Dexpanthenol", "D03AX03", "Skin barrier and wound support."),
                new SubstanceSeed("neutral", "Device or non-medicinal product", "N/A", "Non-medicinal pharmacy product")
        );

        Map<String, Substance> substances = new LinkedHashMap<>();
        for (SubstanceSeed seed : seeds) {
            Substance substance = new Substance();
            substance.setInn(seed.inn());
            substance.setCommonName(seed.commonName());
            substance.setAtcCode(seed.atcCode());
            substance.setDescription(seed.description());
            substances.put(seed.inn(), substanceRepository.save(substance));
        }
        return substances;
    }

    private List<Product> seedProducts(Map<String, Category> categories, Map<String, Substance> substances) {
        List<ProductSeed> seeds = productSeeds();
        List<Product> products = new ArrayList<>();
        for (int i = 0; i < seeds.size(); i++) {
            ProductSeed seed = seeds.get(i);
            Product product = new Product();
            product.setName(seed.name());
            product.setBarcode("387100" + String.format("%07d", i + 1));
            product.setBrandName(seed.brandName());
            product.setManufacturer(seed.manufacturer());
            product.setPrice(seed.price());
            product.setDescription(seed.description());
            product.setCategory(categories.get(seed.categoryName()));
            product.setRequiresPrescription(seed.requiresPrescription());
            product.setProductType(seed.productType());
            product.setImageUrl(productImageUrl(seed.name(), i));
            product.setPackageSize(seed.packageSize());
            product.setSubstances(List.of(substances.get(seed.substanceInn())));
            product.setIsActive(true);
            products.add(productRepository.save(product));
        }
        return products;
    }

    private void seedInteractionsAndContraindications(Map<String, Substance> substances) {
        saveInteraction(substances, "ibuprofen", "warfarin", DrugInteraction.SeverityLevel.MAJOR,
                "Ibuprofen can increase bleeding risk when used with warfarin.",
                "Avoid combination where possible. Consider paracetamol and monitor INR if unavoidable.");
        saveInteraction(substances, "omeprazole", "warfarin", DrugInteraction.SeverityLevel.MODERATE,
                "Omeprazole may increase anticoagulant effect in some patients.",
                "Monitor INR when starting or stopping omeprazole.");
        saveInteraction(substances, "clarithromycin", "atorvastatin", DrugInteraction.SeverityLevel.MAJOR,
                "Clarithromycin can increase atorvastatin exposure and myopathy risk.",
                "Temporarily stop atorvastatin or choose an alternative antibiotic.");
        saveInteraction(substances, "pseudoephedrine", "amlodipine", DrugInteraction.SeverityLevel.MODERATE,
                "Pseudoephedrine can raise blood pressure and reduce antihypertensive control.",
                "Use cautiously in patients with hypertension.");
        saveInteraction(substances, "doxycycline", "magnesium", DrugInteraction.SeverityLevel.MODERATE,
                "Magnesium can reduce doxycycline absorption.",
                "Separate dosing by at least two to three hours.");
        saveInteraction(substances, "acetylsalicylic acid", "warfarin", DrugInteraction.SeverityLevel.MAJOR,
                "Combined antiplatelet and anticoagulant effect increases bleeding risk.",
                "Use only with explicit clinical indication and close monitoring.");

        saveContraindication(substances, "ibuprofen", Contraindication.ContraindicationType.PREGNANCY,
                "Pregnancy, especially third trimester", Contraindication.SeverityType.ABSOLUTE,
                "NSAIDs can cause fetal renal dysfunction and premature closure of ductus arteriosus.");
        saveContraindication(substances, "amoxicillin", Contraindication.ContraindicationType.ALLERGY,
                "Penicillin allergy", Contraindication.SeverityType.ABSOLUTE,
                "Patients with penicillin allergy are at risk of severe allergic reaction.");
        saveContraindication(substances, "warfarin", Contraindication.ContraindicationType.DISEASE,
                "Active bleeding", Contraindication.SeverityType.ABSOLUTE,
                "Warfarin is contraindicated during active bleeding or severe bleeding tendency.");
        saveContraindication(substances, "pseudoephedrine", Contraindication.ContraindicationType.DISEASE,
                "Severe uncontrolled hypertension", Contraindication.SeverityType.RELATIVE,
                "Sympathomimetic decongestants may increase blood pressure.");
        saveContraindication(substances, "ciprofloxacin", Contraindication.ContraindicationType.CONDITION,
                "History of quinolone tendon disorder", Contraindication.SeverityType.RELATIVE,
                "Fluoroquinolones can increase risk of tendon injury.");
    }

    private void seedSubstitutes(List<Product> products) {
        saveSubstitute(products.get(0), products.get(1), ProductSubstitute.SubstituteType.GENERIC, true,
                "Same active substance and dose at a lower price.");
        saveSubstitute(products.get(0), products.get(2), ProductSubstitute.SubstituteType.THERAPEUTIC, false,
                "Alternative pain and fever option when NSAIDs are not appropriate.");
        saveSubstitute(products.get(20), products.get(21), ProductSubstitute.SubstituteType.THERAPEUTIC, false,
                "Alternative proton pump inhibitor in the same therapeutic class.");
        saveSubstitute(products.get(40), products.get(41), ProductSubstitute.SubstituteType.THERAPEUTIC, false,
                "Non-sedating antihistamine alternative.");
        saveSubstitute(products.get(60), products.get(62), ProductSubstitute.SubstituteType.THERAPEUTIC, false,
                "Alternative cardiovascular maintenance medicine where clinically appropriate.");
    }

    private void saveInteraction(Map<String, Substance> substances, String substanceA, String substanceB,
                                 DrugInteraction.SeverityLevel severity, String description, String recommendation) {
        DrugInteraction interaction = new DrugInteraction();
        interaction.setSubstanceA(substances.get(substanceA));
        interaction.setSubstanceB(substances.get(substanceB));
        interaction.setSeverity(severity);
        interaction.setDescription(description);
        interaction.setClinicalRecommendation(recommendation);
        drugInteractionRepository.save(interaction);
    }

    private void saveContraindication(Map<String, Substance> substances, String substanceInn,
                                      Contraindication.ContraindicationType type, String condition,
                                      Contraindication.SeverityType severity, String description) {
        Contraindication contraindication = new Contraindication();
        contraindication.setSubstance(substances.get(substanceInn));
        contraindication.setType(type);
        contraindication.setConditionName(condition);
        contraindication.setSeverityType(severity);
        contraindication.setDescription(description);
        contraindicationRepository.save(contraindication);
    }

    private void saveSubstitute(Product original, Product substitute, ProductSubstitute.SubstituteType type,
                                boolean equivalent, String note) {
        ProductSubstitute productSubstitute = new ProductSubstitute();
        productSubstitute.setOriginalProduct(original);
        productSubstitute.setSubstituteProduct(substitute);
        productSubstitute.setSubstituteType(type);
        productSubstitute.setIsTherapeuticEquivalent(equivalent);
        productSubstitute.setNote(note);
        productSubstituteRepository.save(productSubstitute);
    }

    private List<ProductSeed> productSeeds() {
        return List.of(
                p("Brufen 400mg tablets", "Analgesics and Antipyretics", "ibuprofen", Product.ProductType.MEDICATION, false, "4.50", "30 tablets", "Abbott", "Brufen"),
                p("Ibuprofen 400mg tablets", "Analgesics and Antipyretics", "ibuprofen", Product.ProductType.MEDICATION, false, "2.80", "30 tablets", "Bosnalijek", null),
                p("Panadol 500mg tablets", "Analgesics and Antipyretics", "paracetamol", Product.ProductType.MEDICATION, false, "3.20", "20 tablets", "Haleon", "Panadol"),
                p("Paracetamol 500mg tablets", "Analgesics and Antipyretics", "paracetamol", Product.ProductType.MEDICATION, false, "2.40", "20 tablets", "Hemofarm", null),
                p("Aspirin Protect 100mg tablets", "Analgesics and Antipyretics", "acetylsalicylic acid", Product.ProductType.MEDICATION, false, "5.90", "30 tablets", "Bayer", "Aspirin"),
                p("Nalgesin S 220mg tablets", "Analgesics and Antipyretics", "naproxen", Product.ProductType.MEDICATION, false, "6.30", "20 tablets", "Krka", "Nalgesin"),
                p("Daleron C granules", "Analgesics and Antipyretics", "paracetamol", Product.ProductType.MEDICATION, false, "4.10", "10 sachets", "Krka", "Daleron"),
                p("Nurofen Express 200mg capsules", "Analgesics and Antipyretics", "ibuprofen", Product.ProductType.MEDICATION, false, "6.70", "16 capsules", "Reckitt", "Nurofen"),
                p("Voltaren Emulgel 1% gel", "Analgesics and Antipyretics", "diclofenac", Product.ProductType.MEDICATION, false, "8.90", "50 g", "Haleon", "Voltaren"),
                p("Deep Relief Ibuprofen gel", "Analgesics and Antipyretics", "ibuprofen", Product.ProductType.MEDICATION, false, "7.80", "50 g", "Mentholatum", "Deep Relief"),
                p("Amoxicillin 500mg capsules", "Antibiotics", "amoxicillin", Product.ProductType.MEDICATION, true, "8.90", "16 capsules", "Bosnalijek", null),
                p("Azithromycin 500mg tablets", "Antibiotics", "azithromycin", Product.ProductType.MEDICATION, true, "11.80", "3 tablets", "Pliva", null),
                p("Cefalexin 500mg capsules", "Antibiotics", "cefalexin", Product.ProductType.MEDICATION, true, "9.70", "16 capsules", "Hemofarm", null),
                p("Doxycycline 100mg capsules", "Antibiotics", "doxycycline", Product.ProductType.MEDICATION, true, "7.90", "10 capsules", "Belupo", null),
                p("Ciprofloxacin 500mg tablets", "Antibiotics", "ciprofloxacin", Product.ProductType.MEDICATION, true, "10.40", "10 tablets", "Sandoz", null),
                p("Clarithromycin 500mg tablets", "Antibiotics", "clarithromycin", Product.ProductType.MEDICATION, true, "13.50", "14 tablets", "Krka", null),
                p("Metronidazole 400mg tablets", "Antibiotics", "metronidazole", Product.ProductType.MEDICATION, true, "6.90", "20 tablets", "Galenika", null),
                p("Nitrofurantoin 100mg capsules", "Antibiotics", "nitrofurantoin", Product.ProductType.MEDICATION, true, "8.20", "20 capsules", "Belupo", null),
                p("Fusidic Acid 2% cream", "Antibiotics", "neutral", Product.ProductType.MEDICATION, true, "9.30", "15 g", "Leo Pharma", null),
                p("Mupirocin 2% ointment", "Antibiotics", "neutral", Product.ProductType.MEDICATION, true, "10.10", "15 g", "GSK", null),
                p("Omeprazole 20mg capsules", "Gastrointestinal Medications", "omeprazole", Product.ProductType.MEDICATION, false, "7.10", "14 capsules", "AstraZeneca", "Losec"),
                p("Pantoprazole 40mg tablets", "Gastrointestinal Medications", "pantoprazole", Product.ProductType.MEDICATION, false, "7.60", "14 tablets", "Takeda", null),
                p("Controloc 20mg tablets", "Gastrointestinal Medications", "pantoprazole", Product.ProductType.MEDICATION, false, "8.40", "14 tablets", "Takeda", "Controloc"),
                p("Loperamide 2mg capsules", "Gastrointestinal Medications", "loperamide", Product.ProductType.MEDICATION, false, "3.80", "10 capsules", "Sandoz", null),
                p("Smecta sachets", "Gastrointestinal Medications", "neutral", Product.ProductType.OTHER, false, "5.40", "10 sachets", "Ipsen", "Smecta"),
                p("Probiotic Complex capsules", "Gastrointestinal Medications", "probiotic blend", Product.ProductType.SUPPLEMENT, false, "11.90", "20 capsules", "PharmaS", null),
                p("Buscopan 10mg tablets", "Gastrointestinal Medications", "neutral", Product.ProductType.MEDICATION, false, "6.20", "20 tablets", "Sanofi", "Buscopan"),
                p("Rennie chewable tablets", "Gastrointestinal Medications", "neutral", Product.ProductType.MEDICATION, false, "4.90", "24 tablets", "Bayer", "Rennie"),
                p("Gaviscon Advance liquid", "Gastrointestinal Medications", "neutral", Product.ProductType.MEDICATION, false, "8.60", "150 ml", "Reckitt", "Gaviscon"),
                p("Lactulose syrup 200ml", "Gastrointestinal Medications", "neutral", Product.ProductType.MEDICATION, false, "5.80", "200 ml", "Mylan", null),
                p("Vitamin C 1000mg effervescent tablets", "Vitamins and Supplements", "ascorbic acid", Product.ProductType.SUPPLEMENT, false, "5.60", "20 effervescent tablets", "Atlantic Grupa", "Cedevita"),
                p("Vitamin D3 2000 IU capsules", "Vitamins and Supplements", "colecalciferol", Product.ProductType.SUPPLEMENT, false, "8.30", "60 capsules", "Doppelherz", null),
                p("Magnesium 375mg tablets", "Vitamins and Supplements", "magnesium", Product.ProductType.SUPPLEMENT, false, "9.20", "30 tablets", "Doppelherz", "Magnesium Direct"),
                p("Centrum multivitamin tablets", "Vitamins and Supplements", "neutral", Product.ProductType.SUPPLEMENT, false, "14.90", "30 tablets", "Haleon", "Centrum"),
                p("Omega-3 1000mg softgels", "Vitamins and Supplements", "omega-3-acid ethyl esters", Product.ProductType.SUPPLEMENT, false, "12.60", "60 softgels", "Solgar", null),
                p("Zinc 15mg tablets", "Vitamins and Supplements", "zinc", Product.ProductType.SUPPLEMENT, false, "6.40", "30 tablets", "PharmaS", null),
                p("B-Complex forte tablets", "Vitamins and Supplements", "neutral", Product.ProductType.SUPPLEMENT, false, "7.20", "30 tablets", "Krka", null),
                p("Iron Plus capsules", "Vitamins and Supplements", "neutral", Product.ProductType.SUPPLEMENT, false, "8.80", "30 capsules", "Belupo", null),
                p("Calcium D3 chewables", "Vitamins and Supplements", "colecalciferol", Product.ProductType.SUPPLEMENT, false, "9.40", "40 chewables", "Doppelherz", null),
                p("Electrolyte rehydration sachets", "Vitamins and Supplements", "neutral", Product.ProductType.SUPPLEMENT, false, "5.30", "10 sachets", "PharmaFlow", null),
                p("Cetirizine 10mg tablets", "Allergy Relief", "cetirizine", Product.ProductType.MEDICATION, false, "4.90", "10 tablets", "Krka", "Letizen"),
                p("Loratadine 10mg tablets", "Allergy Relief", "loratadine", Product.ProductType.MEDICATION, false, "5.30", "10 tablets", "Bayer", "Claritin"),
                p("Aerius 5mg tablets", "Allergy Relief", "desloratadine", Product.ProductType.MEDICATION, false, "7.70", "10 tablets", "Organon", "Aerius"),
                p("Flixonase nasal spray", "Allergy Relief", "fluticasone", Product.ProductType.MEDICATION, false, "12.80", "60 doses", "GSK", "Flixonase"),
                p("Aqua Maris nasal spray", "Allergy Relief", "neutral", Product.ProductType.OTHER, false, "6.40", "30 ml", "Jadran", "Aqua Maris"),
                p("Allergy Eye Drops", "Allergy Relief", "neutral", Product.ProductType.MEDICATION, false, "5.90", "10 ml", "PharmaFlow", null),
                p("Pseudoephedrine 60mg tablets", "Allergy Relief", "pseudoephedrine", Product.ProductType.MEDICATION, false, "4.60", "12 tablets", "Hemofarm", null),
                p("Xylometazoline nasal spray", "Allergy Relief", "xylometazoline", Product.ProductType.MEDICATION, false, "5.10", "10 ml", "Sandoz", null),
                p("Saline nasal rinse kit", "Allergy Relief", "neutral", Product.ProductType.OTHER, false, "9.90", "1 kit", "NeilMed", null),
                p("Sinus Relief capsules", "Allergy Relief", "pseudoephedrine", Product.ProductType.MEDICATION, false, "6.50", "16 capsules", "PharmaFlow", null),
                p("Dry Cough Syrup 150ml", "Cold, Flu and Respiratory", "dextromethorphan", Product.ProductType.MEDICATION, false, "6.80", "150 ml", "Belupo", "Tussidex"),
                p("Ivy Leaf Cough Syrup 100ml", "Cold, Flu and Respiratory", "neutral", Product.ProductType.OTHER, false, "7.20", "100 ml", "Engelhard", "Prospan"),
                p("Strepsils Honey Lemon lozenges", "Cold, Flu and Respiratory", "neutral", Product.ProductType.OTHER, false, "4.80", "24 lozenges", "Reckitt", "Strepsils"),
                p("Isla-Mint throat pastilles", "Cold, Flu and Respiratory", "neutral", Product.ProductType.OTHER, false, "5.20", "30 pastilles", "Engelhard", "Isla"),
                p("ACC 600mg effervescent tablets", "Cold, Flu and Respiratory", "acetylcysteine", Product.ProductType.MEDICATION, false, "8.10", "10 tablets", "Sandoz", "ACC"),
                p("Bronchostop syrup", "Cold, Flu and Respiratory", "neutral", Product.ProductType.OTHER, false, "8.70", "120 ml", "Kwizda", "Bronchostop"),
                p("Vicks VapoRub 50g", "Cold, Flu and Respiratory", "neutral", Product.ProductType.OTHER, false, "6.60", "50 g", "P&G", "Vicks"),
                p("Theraflu powder sachets", "Cold, Flu and Respiratory", "paracetamol", Product.ProductType.MEDICATION, false, "7.50", "10 sachets", "Haleon", "Theraflu"),
                p("Olynth nasal spray", "Cold, Flu and Respiratory", "xylometazoline", Product.ProductType.MEDICATION, false, "5.70", "10 ml", "Johnson & Johnson", "Olynth"),
                p("Sore Throat Spray 30ml", "Cold, Flu and Respiratory", "neutral", Product.ProductType.OTHER, false, "6.10", "30 ml", "PharmaFlow", null),
                p("Warfarin 5mg tablets", "Cardiovascular Care", "warfarin", Product.ProductType.MEDICATION, true, "6.40", "30 tablets", "Bosnalijek", null),
                p("Amlodipine 5mg tablets", "Cardiovascular Care", "amlodipine", Product.ProductType.MEDICATION, true, "5.90", "30 tablets", "Hemofarm", null),
                p("Losartan 50mg tablets", "Cardiovascular Care", "losartan", Product.ProductType.MEDICATION, true, "7.40", "30 tablets", "Krka", null),
                p("Atorvastatin 20mg tablets", "Cardiovascular Care", "atorvastatin", Product.ProductType.MEDICATION, true, "8.30", "30 tablets", "Sandoz", null),
                p("Bisoprolol 5mg tablets", "Cardiovascular Care", "bisoprolol", Product.ProductType.MEDICATION, true, "5.50", "30 tablets", "Merck", null),
                p("Ramipril 5mg capsules", "Cardiovascular Care", "ramipril", Product.ProductType.MEDICATION, true, "6.20", "30 capsules", "Sanofi", null),
                p("Aspirin Cardio 100mg tablets", "Cardiovascular Care", "acetylsalicylic acid", Product.ProductType.MEDICATION, false, "5.80", "30 tablets", "Bayer", "Aspirin Cardio"),
                p("Furosemide 40mg tablets", "Cardiovascular Care", "furosemide", Product.ProductType.MEDICATION, true, "4.90", "20 tablets", "Galenika", null),
                p("Rosuvastatin 10mg tablets", "Cardiovascular Care", "atorvastatin", Product.ProductType.MEDICATION, true, "9.10", "30 tablets", "AstraZeneca", null),
                p("Nitroglycerin spray", "Cardiovascular Care", "neutral", Product.ProductType.MEDICATION, true, "13.90", "200 doses", "G. Pohl-Boskamp", null),
                p("Hydrocortisone 1% cream", "Dermatology and First Aid", "hydrocortisone", Product.ProductType.MEDICATION, false, "5.80", "20 g", "Galenika", null),
                p("Bepanthen ointment", "Dermatology and First Aid", "dexpanthenol", Product.ProductType.OTHER, false, "7.30", "30 g", "Bayer", "Bepanthen"),
                p("Antiseptic Spray 100ml", "Dermatology and First Aid", "povidone iodine", Product.ProductType.OTHER, false, "7.50", "100 ml", "Mundipharma", "Betadine"),
                p("Betadine solution 100ml", "Dermatology and First Aid", "povidone iodine", Product.ProductType.OTHER, false, "6.90", "100 ml", "Mundipharma", "Betadine"),
                p("Sterile Gauze Pads", "Dermatology and First Aid", "neutral", Product.ProductType.OTHER, false, "2.90", "10 pads", "Tosama", null),
                p("Elastic Bandage 8cm", "Dermatology and First Aid", "neutral", Product.ProductType.OTHER, false, "3.40", "1 bandage", "Hartmann", null),
                p("Medical Plasters assorted", "Dermatology and First Aid", "neutral", Product.ProductType.OTHER, false, "3.20", "30 plasters", "Hansaplast", null),
                p("Burn Gel 50ml", "Dermatology and First Aid", "neutral", Product.ProductType.OTHER, false, "4.70", "50 ml", "PharmaFlow", null),
                p("Wound Cleansing Wipes", "Dermatology and First Aid", "neutral", Product.ProductType.OTHER, false, "3.90", "20 wipes", "Hartmann", null),
                p("Clotrimazole 1% cream", "Dermatology and First Aid", "clotrimazole", Product.ProductType.MEDICATION, false, "5.40", "20 g", "Sandoz", null),
                p("Digital Thermometer", "Medical Devices", "neutral", Product.ProductType.MEDICAL_DEVICE, false, "12.90", "1 device", "Microlife", null),
                p("Blood Pressure Monitor", "Medical Devices", "neutral", Product.ProductType.MEDICAL_DEVICE, false, "59.90", "1 device", "Omron", null),
                p("Pulse Oximeter", "Medical Devices", "neutral", Product.ProductType.MEDICAL_DEVICE, false, "24.90", "1 device", "Beurer", null),
                p("Glucose Meter Starter Kit", "Medical Devices", "neutral", Product.ProductType.MEDICAL_DEVICE, false, "34.90", "1 kit", "Accu-Chek", null),
                p("Lancets 100 pack", "Medical Devices", "neutral", Product.ProductType.MEDICAL_DEVICE, false, "8.90", "100 lancets", "Accu-Chek", null),
                p("Test Strips 50 pack", "Medical Devices", "neutral", Product.ProductType.MEDICAL_DEVICE, false, "19.90", "50 strips", "Accu-Chek", null),
                p("Nebulizer Compressor", "Medical Devices", "neutral", Product.ProductType.MEDICAL_DEVICE, false, "49.90", "1 device", "Omron", null),
                p("Heating Pad", "Medical Devices", "neutral", Product.ProductType.MEDICAL_DEVICE, false, "29.90", "1 pad", "Beurer", null),
                p("Pill Organizer weekly", "Medical Devices", "neutral", Product.ProductType.MEDICAL_DEVICE, false, "4.50", "1 organizer", "PharmaFlow", null),
                p("Pregnancy Test twin pack", "Medical Devices", "neutral", Product.ProductType.MEDICAL_DEVICE, false, "6.90", "2 tests", "Clearblue", null),
                p("La Roche-Posay Cicaplast Balm", "Dermocosmetics", "neutral", Product.ProductType.COSMETIC, false, "13.90", "40 ml", "La Roche-Posay", "Cicaplast"),
                p("Eucerin UreaRepair Lotion", "Dermocosmetics", "neutral", Product.ProductType.COSMETIC, false, "15.90", "250 ml", "Eucerin", "UreaRepair"),
                p("Bioderma Atoderm Shower Gel", "Dermocosmetics", "neutral", Product.ProductType.COSMETIC, false, "12.40", "200 ml", "Bioderma", "Atoderm"),
                p("Avene Thermal Water Spray", "Dermocosmetics", "neutral", Product.ProductType.COSMETIC, false, "8.70", "150 ml", "Avene", null),
                p("Sunscreen SPF50 Sensitive", "Dermocosmetics", "neutral", Product.ProductType.COSMETIC, false, "14.80", "50 ml", "Eucerin", null),
                p("Baby Diaper Cream", "Dermocosmetics", "neutral", Product.ProductType.COSMETIC, false, "6.60", "100 ml", "Becutan", null),
                p("Lip Balm SPF30", "Dermocosmetics", "neutral", Product.ProductType.COSMETIC, false, "3.80", "4.8 g", "Labello", null),
                p("Hand Sanitizer Gel 500ml", "Dermocosmetics", "neutral", Product.ProductType.OTHER, false, "5.20", "500 ml", "PharmaFlow", null),
                p("Micellar Cleansing Water", "Dermocosmetics", "neutral", Product.ProductType.COSMETIC, false, "9.90", "250 ml", "Bioderma", null),
                p("Anti-Dandruff Shampoo", "Dermocosmetics", "neutral", Product.ProductType.COSMETIC, false, "7.90", "200 ml", "Vichy", null)
        );
    }

    private ProductSeed p(String name, String categoryName, String substanceInn, Product.ProductType type,
                          boolean rx, String price, String packageSize, String manufacturer, String brandName) {
        return new ProductSeed(
                name,
                categoryName,
                substanceInn,
                type,
                rx,
                new BigDecimal(price),
                packageSize,
                manufacturer,
                brandName,
                "%s by %s. Demo catalog product for pharmacy e-commerce browsing, reservations and order workflows."
                        .formatted(name, manufacturer)
        );
    }

    private String imageSlug(String value) {
        return value.toLowerCase()
                .replace("%", "")
                .replace("+", "plus")
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }

    private String productImageUrl(String name, int index) {
        if (index == 0) {
            return DEMO_IMAGE_PATH + "brufen-400-tablets.jpg";
        }
        if (index == 1) {
            return DEMO_IMAGE_PATH + "ibuprofen-400-tablets.jpg";
        }
        if (index == 2) {
            return DEMO_IMAGE_PATH + "panadol-500-tablets.jpg";
        }

        return DEMO_IMAGE_PATH + imageSlug(name) + ".png";
    }

    private record SubstanceSeed(String inn, String commonName, String atcCode, String description) {
    }

    private record ProductSeed(String name, String categoryName, String substanceInn, Product.ProductType productType,
                               boolean requiresPrescription, BigDecimal price, String packageSize, String manufacturer,
                               String brandName, String description) {
    }
}
