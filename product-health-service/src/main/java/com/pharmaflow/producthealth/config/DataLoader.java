package com.pharmaflow.producthealth.config;

import com.pharmaflow.producthealth.models.*;
import com.pharmaflow.producthealth.repositories.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final CategoryRepository categoryRepository;
    private final SubstanceRepository substanceRepository;
    private final ProductRepository productRepository;
    private final DrugInteractionRepository drugInteractionRepository;
    private final ContraindicationRepository contraindicationRepository;
    private final ProductSubstituteRepository productSubstituteRepository;

    @Override
    @Transactional
    public void run(String... args) {
        System.out.println("=== PharmaFlow: Loading initial data ===");

        if (categoryRepository.count() > 0) {
            System.out.println("=== Data already exists, skipping seed ===");
            return;
        }

        // ─── 1. CATEGORIES ───────────────────────────────────────────────

        Category analgesics = new Category();
        analgesics.setName("Analgesics and Antipyretics");
        analgesics.setDescription("Medications for pain relief and fever reduction");
        categoryRepository.save(analgesics);

        Category antibiotics = new Category();
        antibiotics.setName("Antibiotics");
        antibiotics.setDescription("Medications for treating bacterial infections");
        categoryRepository.save(antibiotics);

        Category vitamins = new Category();
        vitamins.setName("Vitamins and Supplements");
        vitamins.setDescription("Vitamins, minerals and dietary supplements");
        categoryRepository.save(vitamins);

        Category gastrointestinal = new Category();
        gastrointestinal.setName("Gastrointestinal Medications");
        gastrointestinal.setDescription("Medications for the digestive system");
        categoryRepository.save(gastrointestinal);

        System.out.println("✓ Categories saved: " + categoryRepository.count());

        // ─── 2. SUBSTANCES ───────────────────────────────────────────────

        Substance ibuprofen = new Substance();
        ibuprofen.setInn("ibuprofen");
        ibuprofen.setCommonName("Ibuprofen");
        ibuprofen.setAtcCode("M01AE01");
        ibuprofen.setDescription("Non-steroidal anti-inflammatory drug (NSAID). Analgesic, antipyretic and anti-inflammatory.");
        substanceRepository.save(ibuprofen);

        Substance paracetamol = new Substance();
        paracetamol.setInn("paracetamol");
        paracetamol.setCommonName("Paracetamol (Acetaminophen)");
        paracetamol.setAtcCode("N02BE01");
        paracetamol.setDescription("Analgesic and antipyretic. No anti-inflammatory effect, suitable for children and pregnant women.");
        substanceRepository.save(paracetamol);

        Substance amoxicillin = new Substance();
        amoxicillin.setInn("amoxicillin");
        amoxicillin.setCommonName("Amoxicillin");
        amoxicillin.setAtcCode("J01CA04");
        amoxicillin.setDescription("Broad-spectrum penicillin antibiotic for bacterial infections.");
        substanceRepository.save(amoxicillin);

        Substance warfarin = new Substance();
        warfarin.setInn("warfarin");
        warfarin.setCommonName("Warfarin");
        warfarin.setAtcCode("B01AA03");
        warfarin.setDescription("Anticoagulant used for prevention of thrombosis and embolism.");
        substanceRepository.save(warfarin);

        Substance omeprazole = new Substance();
        omeprazole.setInn("omeprazole");
        omeprazole.setCommonName("Omeprazole");
        omeprazole.setAtcCode("A02BC01");
        omeprazole.setDescription("Proton pump inhibitor, reduces gastric acid secretion.");
        substanceRepository.save(omeprazole);

        Substance vitaminC = new Substance();
        vitaminC.setInn("ascorbic acid");
        vitaminC.setCommonName("Vitamin C (Ascorbic Acid)");
        vitaminC.setAtcCode("A11GA01");
        vitaminC.setDescription("Essential vitamin, antioxidant, necessary for the immune system.");
        substanceRepository.save(vitaminC);

        System.out.println("✓ Substances saved: " + substanceRepository.count());

        // ─── 3. PRODUCTS ─────────────────────────────────────────────────

        // Branded ibuprofen
        Product brufen = new Product();
        brufen.setName("Brufen 400mg tablets");
        brufen.setBarcode("3838989522018");
        brufen.setBrandName("Brufen");
        brufen.setManufacturer("Abbott");
        brufen.setPrice(new BigDecimal("4.50"));
        brufen.setDescription("Brufen 400mg contains ibuprofen. Used for relief of mild to moderate pain and reduction of fever.");
        brufen.setCategory(analgesics);
        brufen.setRequiresPrescription(false);
        brufen.setProductType(Product.ProductType.MEDICATION);
        brufen.setPackageSize("30 tablets");
        brufen.setSubstances(List.of(ibuprofen));
        brufen.setIsActive(true);
        productRepository.save(brufen);

        // Generic ibuprofen (substitute for Brufen)
        Product ibuprofenGeneric = new Product();
        ibuprofenGeneric.setName("Ibuprofen 400mg tablets");
        ibuprofenGeneric.setBarcode("3858881733217");
        ibuprofenGeneric.setBrandName(null);
        ibuprofenGeneric.setManufacturer("Bosnalijek");
        ibuprofenGeneric.setPrice(new BigDecimal("2.80"));
        ibuprofenGeneric.setDescription("Generic ibuprofen 400mg. Therapeutic equivalent of branded Brufen at a lower price.");
        ibuprofenGeneric.setCategory(analgesics);
        ibuprofenGeneric.setRequiresPrescription(false);
        ibuprofenGeneric.setProductType(Product.ProductType.MEDICATION);
        ibuprofenGeneric.setPackageSize("30 tablets");
        ibuprofenGeneric.setSubstances(List.of(ibuprofen));
        ibuprofenGeneric.setIsActive(true);
        productRepository.save(ibuprofenGeneric);

        // Panadol
        Product panadol = new Product();
        panadol.setName("Panadol 500mg tablets");
        panadol.setBarcode("5000157021718");
        panadol.setBrandName("Panadol");
        panadol.setManufacturer("Haleon");
        panadol.setPrice(new BigDecimal("3.20"));
        panadol.setDescription("Panadol 500mg contains paracetamol. Used for pain relief and fever reduction. Suitable for children over 12 years.");
        panadol.setCategory(analgesics);
        panadol.setRequiresPrescription(false);
        panadol.setProductType(Product.ProductType.MEDICATION);
        panadol.setPackageSize("20 tablets");
        panadol.setSubstances(List.of(paracetamol));
        panadol.setIsActive(true);
        productRepository.save(panadol);

        // Amoxicillin (prescription required)
        Product amoxicillinProduct = new Product();
        amoxicillinProduct.setName("Amoxicillin 500mg capsules");
        amoxicillinProduct.setBarcode("3858881620012");
        amoxicillinProduct.setBrandName("Amoxicillin");
        amoxicillinProduct.setManufacturer("Bosnalijek");
        amoxicillinProduct.setPrice(new BigDecimal("8.90"));
        amoxicillinProduct.setDescription("Amoxicillin 500mg is a penicillin group antibiotic. Treats bacterial infections of the respiratory tract, urinary tract and ear.");
        amoxicillinProduct.setCategory(antibiotics);
        amoxicillinProduct.setRequiresPrescription(true);
        amoxicillinProduct.setProductType(Product.ProductType.MEDICATION);
        amoxicillinProduct.setPackageSize("16 capsules");
        amoxicillinProduct.setSubstances(List.of(amoxicillin));
        amoxicillinProduct.setIsActive(true);
        productRepository.save(amoxicillinProduct);

        // Warfarin (prescription required)
        Product warfarinProduct = new Product();
        warfarinProduct.setName("Warfarin 5mg tablets");
        warfarinProduct.setBarcode("3858881711215");
        warfarinProduct.setBrandName("Warfarin");
        warfarinProduct.setManufacturer("Bosnalijek");
        warfarinProduct.setPrice(new BigDecimal("6.40"));
        warfarinProduct.setDescription("Warfarin 5mg is an anticoagulant. Used for prevention and treatment of deep vein thrombosis and pulmonary embolism.");
        warfarinProduct.setCategory(analgesics);
        warfarinProduct.setRequiresPrescription(true);
        warfarinProduct.setProductType(Product.ProductType.MEDICATION);
        warfarinProduct.setPackageSize("30 tablets");
        warfarinProduct.setSubstances(List.of(warfarin));
        warfarinProduct.setIsActive(true);
        productRepository.save(warfarinProduct);

        // Omeprazole
        Product omeprazoleProduct = new Product();
        omeprazoleProduct.setName("Omeprazole 20mg capsules");
        omeprazoleProduct.setBarcode("3858889011312");
        omeprazoleProduct.setBrandName("Losec");
        omeprazoleProduct.setManufacturer("AstraZeneca");
        omeprazoleProduct.setPrice(new BigDecimal("7.10"));
        omeprazoleProduct.setDescription("Omeprazole 20mg reduces gastric acid secretion. Used for treatment of gastric ulcers and gastroesophageal reflux.");
        omeprazoleProduct.setCategory(gastrointestinal);
        omeprazoleProduct.setRequiresPrescription(false);
        omeprazoleProduct.setProductType(Product.ProductType.MEDICATION);
        omeprazoleProduct.setPackageSize("14 capsules");
        omeprazoleProduct.setSubstances(List.of(omeprazole));
        omeprazoleProduct.setIsActive(true);
        productRepository.save(omeprazoleProduct);

        // Vitamin C 1000mg
        Product vitaminCProduct = new Product();
        vitaminCProduct.setName("Vitamin C 1000mg effervescent tablets");
        vitaminCProduct.setBarcode("3838957123456");
        vitaminCProduct.setBrandName("Cedevita");
        vitaminCProduct.setManufacturer("Atlantic Grupa");
        vitaminCProduct.setPrice(new BigDecimal("5.60"));
        vitaminCProduct.setDescription("Vitamin C 1000mg in effervescent tablet form. Supports the immune system, reduces fatigue and contributes to normal blood vessel function.");
        vitaminCProduct.setCategory(vitamins);
        vitaminCProduct.setRequiresPrescription(false);
        vitaminCProduct.setProductType(Product.ProductType.SUPPLEMENT);
        vitaminCProduct.setPackageSize("20 effervescent tablets");
        vitaminCProduct.setSubstances(List.of(vitaminC));
        vitaminCProduct.setIsActive(true);
        productRepository.save(vitaminCProduct);

        System.out.println("✓ Products saved: " + productRepository.count());

        // ─── 4. DRUG INTERACTIONS ─────────────────────────────────────────

        // MAJOR: Ibuprofen + Warfarin → increased bleeding risk
        DrugInteraction ibuprofenWarfarin = new DrugInteraction();
        ibuprofenWarfarin.setSubstanceA(ibuprofen);
        ibuprofenWarfarin.setSubstanceB(warfarin);
        ibuprofenWarfarin.setSeverity(DrugInteraction.SeverityLevel.MAJOR);
        ibuprofenWarfarin.setDescription("Ibuprofen (NSAID) enhances the anticoagulant effect of warfarin and increases the risk of gastrointestinal bleeding.");
        ibuprofenWarfarin.setClinicalRecommendation("Avoid combination. If necessary, regularly monitor INR and signs of bleeding. Consider paracetamol as an alternative.");
        drugInteractionRepository.save(ibuprofenWarfarin);

        // MODERATE: Omeprazole + Warfarin → increased anticoagulant effect
        DrugInteraction omeprazoleWarfarin = new DrugInteraction();
        omeprazoleWarfarin.setSubstanceA(omeprazole);
        omeprazoleWarfarin.setSubstanceB(warfarin);
        omeprazoleWarfarin.setSeverity(DrugInteraction.SeverityLevel.MODERATE);
        omeprazoleWarfarin.setDescription("Omeprazole may inhibit warfarin metabolism via CYP2C19 enzyme, potentially increasing the anticoagulant effect.");
        omeprazoleWarfarin.setClinicalRecommendation("Monitor INR values when introducing or discontinuing omeprazole. Adjust warfarin dose as needed.");
        drugInteractionRepository.save(omeprazoleWarfarin);

        // MINOR: Ibuprofen + Amoxicillin
        DrugInteraction ibuprofenAmoxicillin = new DrugInteraction();
        ibuprofenAmoxicillin.setSubstanceA(ibuprofen);
        ibuprofenAmoxicillin.setSubstanceB(amoxicillin);
        ibuprofenAmoxicillin.setSeverity(DrugInteraction.SeverityLevel.MINOR);
        ibuprofenAmoxicillin.setDescription("Ibuprofen may slightly reduce renal elimination of amoxicillin, but this interaction rarely has clinical significance.");
        ibuprofenAmoxicillin.setClinicalRecommendation("Combination is generally safe. Monitor renal function with prolonged use.");
        drugInteractionRepository.save(ibuprofenAmoxicillin);

        System.out.println("✓ Drug interactions saved: " + drugInteractionRepository.count());

        // ─── 5. CONTRAINDICATIONS ─────────────────────────────────────────

        // Ibuprofen contraindicated in pregnancy (3rd trimester)
        Contraindication ibuprofenPregnancy = new Contraindication();
        ibuprofenPregnancy.setSubstance(ibuprofen);
        ibuprofenPregnancy.setType(Contraindication.ContraindicationType.PREGNANCY);
        ibuprofenPregnancy.setConditionName("Pregnancy (especially 3rd trimester)");
        ibuprofenPregnancy.setDescription("NSAIDs including ibuprofen may cause premature closure of ductus arteriosus and fetal renal dysfunction. Absolutely contraindicated from 28th week of pregnancy.");
        ibuprofenPregnancy.setSeverityType(Contraindication.SeverityType.ABSOLUTE);
        contraindicationRepository.save(ibuprofenPregnancy);

        // Ibuprofen contraindicated in peptic ulcer
        Contraindication ibuprofenUlcer = new Contraindication();
        ibuprofenUlcer.setSubstance(ibuprofen);
        ibuprofenUlcer.setType(Contraindication.ContraindicationType.DISEASE);
        ibuprofenUlcer.setConditionName("Active peptic ulcer / gastrointestinal bleeding");
        ibuprofenUlcer.setDescription("Ibuprofen inhibits prostaglandin synthesis that protects gastric mucosa, which may worsen peptic ulcer and cause bleeding.");
        ibuprofenUlcer.setSeverityType(Contraindication.SeverityType.ABSOLUTE);
        contraindicationRepository.save(ibuprofenUlcer);

        // Amoxicillin contraindicated in penicillin allergy
        Contraindication amoxicillinAllergy = new Contraindication();
        amoxicillinAllergy.setSubstance(amoxicillin);
        amoxicillinAllergy.setType(Contraindication.ContraindicationType.ALLERGY);
        amoxicillinAllergy.setConditionName("Allergy to penicillins or cephalosporins");
        amoxicillinAllergy.setDescription("Patients allergic to penicillins have increased risk of allergic reaction (anaphylaxis) to amoxicillin. Cross-reactivity with cephalosporins is possible.");
        amoxicillinAllergy.setSeverityType(Contraindication.SeverityType.ABSOLUTE);
        contraindicationRepository.save(amoxicillinAllergy);

        // Warfarin contraindicated in active bleeding
        Contraindication warfarinBleeding = new Contraindication();
        warfarinBleeding.setSubstance(warfarin);
        warfarinBleeding.setType(Contraindication.ContraindicationType.DISEASE);
        warfarinBleeding.setConditionName("Active bleeding / haemorrhagic diathesis");
        warfarinBleeding.setDescription("Warfarin as an anticoagulant is absolutely contraindicated in all conditions with active bleeding or significantly increased bleeding risk.");
        warfarinBleeding.setSeverityType(Contraindication.SeverityType.ABSOLUTE);
        contraindicationRepository.save(warfarinBleeding);

        // Ibuprofen contraindicated in children under 3 months
        Contraindication ibuprofenInfants = new Contraindication();
        ibuprofenInfants.setSubstance(ibuprofen);
        ibuprofenInfants.setType(Contraindication.ContraindicationType.AGE);
        ibuprofenInfants.setConditionName("Children under 3 months of age");
        ibuprofenInfants.setDescription("Ibuprofen use is not approved for infants under 3 months. Paracetamol is recommended for this age group.");
        ibuprofenInfants.setSeverityType(Contraindication.SeverityType.ABSOLUTE);
        contraindicationRepository.save(ibuprofenInfants);

        System.out.println("✓ Contraindications saved: " + contraindicationRepository.count());

        // ─── 6. PRODUCT SUBSTITUTES ───────────────────────────────────────

        // Brufen → Generic Ibuprofen
        ProductSubstitute brufenGenericSub = new ProductSubstitute();
        brufenGenericSub.setOriginalProduct(brufen);
        brufenGenericSub.setSubstituteProduct(ibuprofenGeneric);
        brufenGenericSub.setSubstituteType(ProductSubstitute.SubstituteType.GENERIC);
        brufenGenericSub.setIsTherapeuticEquivalent(true);
        brufenGenericSub.setNote("Generic equivalent at significantly lower price. Same dose, same active substance.");
        productSubstituteRepository.save(brufenGenericSub);

        // Brufen → Panadol (therapeutic substitute when NSAID is contraindicated)
        ProductSubstitute brufenParacetamolSub = new ProductSubstitute();
        brufenParacetamolSub.setOriginalProduct(brufen);
        brufenParacetamolSub.setSubstituteProduct(panadol);
        brufenParacetamolSub.setSubstituteType(ProductSubstitute.SubstituteType.THERAPEUTIC);
        brufenParacetamolSub.setIsTherapeuticEquivalent(false);
        brufenParacetamolSub.setNote("Recommended substitute for pregnant women, patients with ulcers or on anticoagulant therapy.");
        productSubstituteRepository.save(brufenParacetamolSub);

        System.out.println("✓ Substitutes saved: " + productSubstituteRepository.count());

        System.out.println("=== Initial data loaded successfully! ===");
        System.out.println("    Categories  : " + categoryRepository.count());
        System.out.println("    Substances  : " + substanceRepository.count());
        System.out.println("    Products    : " + productRepository.count());
        System.out.println("    Interactions: " + drugInteractionRepository.count());
        System.out.println("    Contraind.  : " + contraindicationRepository.count());
        System.out.println("    Substitutes : " + productSubstituteRepository.count());
    }
}