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
        System.out.println("=== PharmaFlow: Učitavanje inicijalnih podataka ===");

        // ─── 1. KATEGORIJE ───────────────────────────────────────────────
        Category analgetici = new Category();
        analgetici.setName("Analgetici i antipiretici");
        analgetici.setDescription("Lijekovi za ublažavanje boli i snižavanje temperature");
        categoryRepository.save(analgetici);

        Category antibiotici = new Category();
        antibiotici.setName("Antibiotici");
        antibiotici.setDescription("Lijekovi za liječenje bakterijskih infekcija");
        categoryRepository.save(antibiotici);

        Category vitamini = new Category();
        vitamini.setName("Vitamini i suplementi");
        vitamini.setDescription("Vitamini, minerali i prehrambeni suplementi");
        categoryRepository.save(vitamini);

        Category gastrointestinalni = new Category();
        gastrointestinalni.setName("Gastrointestinalni lijekovi");
        gastrointestinalni.setDescription("Lijekovi za probavni sistem");
        categoryRepository.save(gastrointestinalni);

        System.out.println("✓ Kategorije upisane: " + categoryRepository.count());

        // ─── 2. SUPSTANCE ────────────────────────────────────────────────
        Substance ibuprofen = new Substance();
        ibuprofen.setInn("ibuprofen");
        ibuprofen.setCommonName("Ibuprofen");
        ibuprofen.setAtcCode("M01AE01");
        ibuprofen.setDescription("Nesteroidni protuupalni lijek (NSAID). Djeluje analgetički, antipiretički i protuupalno.");
        substanceRepository.save(ibuprofen);

        Substance paracetamol = new Substance();
        paracetamol.setInn("paracetamol");
        paracetamol.setCommonName("Paracetamol (Acetaminofen)");
        paracetamol.setAtcCode("N02BE01");
        paracetamol.setDescription("Analgetik i antipiretik. Ne djeluje protuupalno, pogodan za djecu i trudnice.");
        substanceRepository.save(paracetamol);

        Substance amoksicilin = new Substance();
        amoksicilin.setInn("amoxicillin");
        amoksicilin.setCommonName("Amoksicilin");
        amoksicilin.setAtcCode("J01CA04");
        amoksicilin.setDescription("Penicilinski antibiotik širokog spektra za bakterijske infekcije.");
        substanceRepository.save(amoksicilin);

        Substance varfarin = new Substance();
        varfarin.setInn("warfarin");
        varfarin.setCommonName("Varfarin");
        varfarin.setAtcCode("B01AA03");
        varfarin.setDescription("Antikoagulans koji se koristi za prevenciju tromboze i embolije.");
        substanceRepository.save(varfarin);

        Substance omeprazol = new Substance();
        omeprazol.setInn("omeprazole");
        omeprazol.setCommonName("Omeprazol");
        omeprazol.setAtcCode("A02BC01");
        omeprazol.setDescription("Inhibitor protonske pumpe, smanjuje lučenje kiseline u želucu.");
        substanceRepository.save(omeprazol);

        Substance vitaminC = new Substance();
        vitaminC.setInn("ascorbic acid");
        vitaminC.setCommonName("Vitamin C (Askorbinska kiselina)");
        vitaminC.setAtcCode("A11GA01");
        vitaminC.setDescription("Esencijalni vitamin, antioksidant, neophodan za imunosni sistem.");
        substanceRepository.save(vitaminC);

        System.out.println("✓ Supstance upisane: " + substanceRepository.count());

        // ─── 3. PROIZVODI ────────────────────────────────────────────────

        // Brufen 400mg (brendirani ibuprofen)
        Product brufen = new Product();
        brufen.setName("Brufen 400mg tablete");
        brufen.setBarcode("3838989522018");
        brufen.setBrandName("Brufen");
        brufen.setManufacturer("Abbott");
        brufen.setPrice(new BigDecimal("4.50"));
        brufen.setDescription("Brufen 400mg sadrži ibuprofen. Primjenjuje se za ublažavanje blage do umjerene boli i snižavanje povišene tjelesne temperature.");
        brufen.setCategory(analgetici);
        brufen.setRequiresPrescription(false);
        brufen.setProductType(Product.ProductType.MEDICATION);
        brufen.setPackageSize("30 tableta");
        brufen.setSubstances(List.of(ibuprofen));
        brufen.setIsActive(true);
        productRepository.save(brufen);

        // Generički ibuprofen (zamjena za Brufen)
        Product ibuprofenGeneric = new Product();
        ibuprofenGeneric.setName("Ibuprofen 400mg tablete");
        ibuprofenGeneric.setBarcode("3858881733217");
        ibuprofenGeneric.setBrandName(null);
        ibuprofenGeneric.setManufacturer("Bosnalijek");
        ibuprofenGeneric.setPrice(new BigDecimal("2.80"));
        ibuprofenGeneric.setDescription("Generički ibuprofen 400mg. Terapijski ekvivalent brendiranom Brufenu po nižoj cijeni.");
        ibuprofenGeneric.setCategory(analgetici);
        ibuprofenGeneric.setRequiresPrescription(false);
        ibuprofenGeneric.setProductType(Product.ProductType.MEDICATION);
        ibuprofenGeneric.setPackageSize("30 tableta");
        ibuprofenGeneric.setSubstances(List.of(ibuprofen));
        ibuprofenGeneric.setIsActive(true);
        productRepository.save(ibuprofenGeneric);

        // Panadol (paracetamol)
        Product panadol = new Product();
        panadol.setName("Panadol 500mg tablete");
        panadol.setBarcode("5000157021718");
        panadol.setBrandName("Panadol");
        panadol.setManufacturer("Haleon");
        panadol.setPrice(new BigDecimal("3.20"));
        panadol.setDescription("Panadol 500mg sadrži paracetamol. Koristi se za ublažavanje boli i snižavanje temperature. Pogodan za djecu stariju od 12 godina.");
        panadol.setCategory(analgetici);
        panadol.setRequiresPrescription(false);
        panadol.setProductType(Product.ProductType.MEDICATION);
        panadol.setPackageSize("20 tableta");
        panadol.setSubstances(List.of(paracetamol));
        panadol.setIsActive(true);
        productRepository.save(panadol);

        // Amoksicilin (na recept)
        Product amoksicilinProduct = new Product();
        amoksicilinProduct.setName("Amoksicilin 500mg kapsule");
        amoksicilinProduct.setBarcode("3858881620012");
        amoksicilinProduct.setBrandName("Amoksicilin");
        amoksicilinProduct.setManufacturer("Bosnalijek");
        amoksicilinProduct.setPrice(new BigDecimal("8.90"));
        amoksicilinProduct.setDescription("Amoksicilin 500mg je antibiotik iz grupe penicilina. Liječi bakterijske infekcije dišnog sistema, urinarnog trakta i uha.");
        amoksicilinProduct.setCategory(antibiotici);
        amoksicilinProduct.setRequiresPrescription(true);
        amoksicilinProduct.setProductType(Product.ProductType.MEDICATION);
        amoksicilinProduct.setPackageSize("16 kapsula");
        amoksicilinProduct.setSubstances(List.of(amoksicilin));
        amoksicilinProduct.setIsActive(true);
        productRepository.save(amoksicilinProduct);

        // Warfarin (na recept)
        Product warfarinProduct = new Product();
        warfarinProduct.setName("Varfarin 5mg tablete");
        warfarinProduct.setBarcode("3858881711215");
        warfarinProduct.setBrandName("Varfarin");
        warfarinProduct.setManufacturer("Bosnalijek");
        warfarinProduct.setPrice(new BigDecimal("6.40"));
        warfarinProduct.setDescription("Varfarin 5mg je antikoagulans. Koristi se za prevenciju i liječenje tromboze dubokih vena i plućne embolije.");
        warfarinProduct.setCategory(analgetici);
        warfarinProduct.setRequiresPrescription(true);
        warfarinProduct.setProductType(Product.ProductType.MEDICATION);
        warfarinProduct.setPackageSize("30 tableta");
        warfarinProduct.setSubstances(List.of(varfarin));
        warfarinProduct.setIsActive(true);
        productRepository.save(warfarinProduct);

        // Omeprazol
        Product omeprazolProduct = new Product();
        omeprazolProduct.setName("Omeprazol 20mg kapsule");
        omeprazolProduct.setBarcode("3858889011312");
        omeprazolProduct.setBrandName("Losec");
        omeprazolProduct.setManufacturer("AstraZeneca");
        omeprazolProduct.setPrice(new BigDecimal("7.10"));
        omeprazolProduct.setDescription("Omeprazol 20mg smanjuje lučenje želučane kiseline. Koristi se za liječenje čira na želucu i gastroezofagealnog refluksa.");
        omeprazolProduct.setCategory(gastrointestinalni);
        omeprazolProduct.setRequiresPrescription(false);
        omeprazolProduct.setProductType(Product.ProductType.MEDICATION);
        omeprazolProduct.setPackageSize("14 kapsula");
        omeprazolProduct.setSubstances(List.of(omeprazol));
        omeprazolProduct.setIsActive(true);
        productRepository.save(omeprazolProduct);

        // Vitamin C 1000mg
        Product vitaminCProduct = new Product();
        vitaminCProduct.setName("Vitamin C 1000mg šumeće tablete");
        vitaminCProduct.setBarcode("3838957123456");
        vitaminCProduct.setBrandName("Cedevita");
        vitaminCProduct.setManufacturer("Atlantic Grupa");
        vitaminCProduct.setPrice(new BigDecimal("5.60"));
        vitaminCProduct.setDescription("Vitamin C 1000mg u obliku šumećih tableta. Podržava imunosni sistem, smanjuje umor i doprinosi normalnoj funkciji krvnih žila.");
        vitaminCProduct.setCategory(vitamini);
        vitaminCProduct.setRequiresPrescription(false);
        vitaminCProduct.setProductType(Product.ProductType.SUPPLEMENT);
        vitaminCProduct.setPackageSize("20 šumećih tableta");
        vitaminCProduct.setSubstances(List.of(vitaminC));
        vitaminCProduct.setIsActive(true);
        productRepository.save(vitaminCProduct);

        System.out.println("✓ Proizvodi upisani: " + productRepository.count());

        // ─── 4. INTERAKCIJE LIJEKOVA ─────────────────────────────────────

        // MAJOR: Ibuprofen + Varfarin → pojačano krvarenje
        DrugInteraction ibuprofenVarfarin = new DrugInteraction();
        ibuprofenVarfarin.setSubstanceA(ibuprofen);
        ibuprofenVarfarin.setSubstanceB(varfarin);
        ibuprofenVarfarin.setSeverity(DrugInteraction.SeverityLevel.MAJOR);
        ibuprofenVarfarin.setDescription("Ibuprofen (NSAID) pojačava antikoagulantni učinak varfarina i povećava rizik od gastrointestinalnog krvarenja.");
        ibuprofenVarfarin.setClinicalRecommendation("Izbjegavati kombinaciju. Ako je neophodna primjena, redovno pratiti INR i znakove krvarenja. Razmotriti paracetamol kao alternativu.");
        drugInteractionRepository.save(ibuprofenVarfarin);

        // MODERATE: Omeprazol + Varfarin → povećan antikoagulantni učinak
        DrugInteraction omeprazolVarfarin = new DrugInteraction();
        omeprazolVarfarin.setSubstanceA(omeprazol);
        omeprazolVarfarin.setSubstanceB(varfarin);
        omeprazolVarfarin.setSeverity(DrugInteraction.SeverityLevel.MODERATE);
        omeprazolVarfarin.setDescription("Omeprazol može inhibirati metabolizam varfarina putem CYP2C19 enzima, što može povećati antikoagulantni učinak.");
        omeprazolVarfarin.setClinicalRecommendation("Pratiti INR vrijednosti pri uvođenju ili ukidanju omeprazola. Prilagoditi dozu varfarina prema potrebi.");
        drugInteractionRepository.save(omeprazolVarfarin);

        // MINOR: Ibuprofen + Amoksicilin (blaga interakcija)
        DrugInteraction ibuprofenAmoksicilin = new DrugInteraction();
        ibuprofenAmoksicilin.setSubstanceA(ibuprofen);
        ibuprofenAmoksicilin.setSubstanceB(amoksicilin);
        ibuprofenAmoksicilin.setSeverity(DrugInteraction.SeverityLevel.MINOR);
        ibuprofenAmoksicilin.setDescription("Ibuprofen može blago smanjiti renalnu eliminaciju amoksicilina, ali ova interakcija rijetko ima klinički značaj.");
        ibuprofenAmoksicilin.setClinicalRecommendation("Kombinacija je generalno sigurna. Pratiti bubrežnu funkciju pri duljoj primjeni.");
        drugInteractionRepository.save(ibuprofenAmoksicilin);

        System.out.println("✓ Interakcije lijekova upisane: " + drugInteractionRepository.count());

        // ─── 5. KONTRAINDIKACIJE ─────────────────────────────────────────

        // Ibuprofen kontraindikovan u trudnoći (3. trimestar)
        Contraindication ibuprofenTrudnoca = new Contraindication();
        ibuprofenTrudnoca.setSubstance(ibuprofen);
        ibuprofenTrudnoca.setType(Contraindication.ContraindicationType.PREGNANCY);
        ibuprofenTrudnoca.setConditionName("Trudnoća (posebno 3. trimestar)");
        ibuprofenTrudnoca.setDescription("NSAID lijekovi uključujući ibuprofen mogu uzrokovati prijevremeno zatvaranje ductus arteriosus i bubrežnu disfunkciju fetusa. Apsolutno kontraindikovan od 28. sedmice trudnoće.");
        ibuprofenTrudnoca.setSeverityType(Contraindication.SeverityType.ABSOLUTE);
        contraindicationRepository.save(ibuprofenTrudnoca);

        // Ibuprofen kontraindikovan kod peptičnog ulkusa
        Contraindication ibuprofenUlkus = new Contraindication();
        ibuprofenUlkus.setSubstance(ibuprofen);
        ibuprofenUlkus.setType(Contraindication.ContraindicationType.DISEASE);
        ibuprofenUlkus.setConditionName("Aktivni peptički ulkus / gastrointestinalno krvarenje");
        ibuprofenUlkus.setDescription("Ibuprofen inhibira sintezu prostaglandina koji štite sluznicu želuca, što može pogoršati peptički ulkus i uzrokovati krvarenje.");
        ibuprofenUlkus.setSeverityType(Contraindication.SeverityType.ABSOLUTE);
        contraindicationRepository.save(ibuprofenUlkus);

        // Amoksicilin kontraindikovan kod alergije na penicilin
        Contraindication amoksicilinAlergija = new Contraindication();
        amoksicilinAlergija.setSubstance(amoksicilin);
        amoksicilinAlergija.setType(Contraindication.ContraindicationType.ALLERGY);
        amoksicilinAlergija.setConditionName("Alergija na peniciline ili cefalosporine");
        amoksicilinAlergija.setDescription("Pacijenti alergični na peniciline imaju povećan rizik od alergijske reakcije (anafilakse) na amoksicilin. Unakrsna reaktivnost sa cefalosporinima je moguća.");
        amoksicilinAlergija.setSeverityType(Contraindication.SeverityType.ABSOLUTE);
        contraindicationRepository.save(amoksicilinAlergija);

        // Varfarin kontraindikovan kod aktivnog krvarenja
        Contraindication varfarinKrvarenje = new Contraindication();
        varfarinKrvarenje.setSubstance(varfarin);
        varfarinKrvarenje.setType(Contraindication.ContraindicationType.DISEASE);
        varfarinKrvarenje.setConditionName("Aktivno krvarenje / hemoragijska dijateza");
        varfarinKrvarenje.setDescription("Varfarin kao antikoagulans apsolutno je kontraindikovan kod svih stanja s aktivnim krvarenjem ili značajno povećanim rizikom od krvarenja.");
        varfarinKrvarenje.setSeverityType(Contraindication.SeverityType.ABSOLUTE);
        contraindicationRepository.save(varfarinKrvarenje);

        // Ibuprofen - djeca ispod 3 meseca
        Contraindication ibuprofenDjeca = new Contraindication();
        ibuprofenDjeca.setSubstance(ibuprofen);
        ibuprofenDjeca.setType(Contraindication.ContraindicationType.AGE);
        ibuprofenDjeca.setConditionName("Djeca ispod 3 mjeseca starosti");
        ibuprofenDjeca.setDescription("Primjena ibuprofena nije odobrena za dojenčad mlađu od 3 mjeseca. Za ovu dobnu skupinu preporučuje se paracetamol.");
        ibuprofenDjeca.setSeverityType(Contraindication.SeverityType.ABSOLUTE);
        contraindicationRepository.save(ibuprofenDjeca);

        System.out.println("✓ Kontraindikacije upisane: " + contraindicationRepository.count());

        // ─── 6. ZAMJENSKI LIJEKOVI ───────────────────────────────────────

        // Brufen → Generički Ibuprofen (zamjena generičkom verzijom)
        ProductSubstitute brufenZamjena = new ProductSubstitute();
        brufenZamjena.setOriginalProduct(brufen);
        brufenZamjena.setSubstituteProduct(ibuprofenGeneric);
        brufenZamjena.setSubstituteType(ProductSubstitute.SubstituteType.GENERIC);
        brufenZamjena.setIsTherapeuticEquivalent(true);
        brufenZamjena.setNote("Generički ekvivalent po znatno nižoj cijeni. Ista doza, ista aktivna supstanca.");
        productSubstituteRepository.save(brufenZamjena);

        // Brufen → Panadol (terapijska zamjena kada je NSAID kontraindiciran)
        ProductSubstitute brufenParacetamolZamjena = new ProductSubstitute();
        brufenParacetamolZamjena.setOriginalProduct(brufen);
        brufenParacetamolZamjena.setSubstituteProduct(panadol);
        brufenParacetamolZamjena.setSubstituteType(ProductSubstitute.SubstituteType.THERAPEUTIC);
        brufenParacetamolZamjena.setIsTherapeuticEquivalent(false);
        brufenParacetamolZamjena.setNote("Preporučena zamjena za trudnice, pacijente s ulkusom ili na antikoagulantnoj terapiji.");
        productSubstituteRepository.save(brufenParacetamolZamjena);

        System.out.println("✓ Zamjenski lijekovi upisani: " + productSubstituteRepository.count());

        System.out.println("=== Inicijalni podaci uspješno učitani! ===");
        System.out.println("    Kategorije  : " + categoryRepository.count());
        System.out.println("    Supstance   : " + substanceRepository.count());
        System.out.println("    Proizvodi   : " + productRepository.count());
        System.out.println("    Interakcije : " + drugInteractionRepository.count());
        System.out.println("    Kontrai.    : " + contraindicationRepository.count());
        System.out.println("    Zamjene     : " + productSubstituteRepository.count());
    }
}