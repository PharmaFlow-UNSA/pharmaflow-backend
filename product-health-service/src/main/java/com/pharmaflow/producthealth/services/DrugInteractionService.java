package com.pharmaflow.producthealth.services;

import com.pharmaflow.producthealth.models.DrugInteraction;
import com.pharmaflow.producthealth.models.Product;
import com.pharmaflow.producthealth.models.Substance;
import com.pharmaflow.producthealth.repositories.DrugInteractionRepository;
import com.pharmaflow.producthealth.repositories.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DrugInteractionService {

    private final DrugInteractionRepository interactionRepository;
    private final ProductRepository productRepository;

    public List<DrugInteraction> getInteractionsForSubstance(Long substanceId) {
        return interactionRepository.findAllBySubstanceId(substanceId);
    }

    /**
     * Provjera interakcija između liste proizvoda koje korisnik želi kupiti.
     * Uzima sve supstance iz svih proizvoda i provjerava svaki par.
     */
    public List<DrugInteraction> checkInteractionsForProducts(List<Long> productIds) {
        List<Substance> allSubstances = new ArrayList<>();

        for (Long productId : productIds) {
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("Proizvod " + productId + " nije pronađen."));
            if (product.getSubstances() != null) {
                allSubstances.addAll(product.getSubstances());
            }
        }

        List<DrugInteraction> foundInteractions = new ArrayList<>();

        // Provjera svakog para supstanci
        for (int i = 0; i < allSubstances.size(); i++) {
            for (int j = i + 1; j < allSubstances.size(); j++) {
                Long idA = allSubstances.get(i).getId();
                Long idB = allSubstances.get(j).getId();
                List<DrugInteraction> interactions = interactionRepository.findInteractionBetween(idA, idB);
                foundInteractions.addAll(interactions);
            }
        }

        return foundInteractions;
    }

    public DrugInteraction saveInteraction(DrugInteraction interaction) {
        return interactionRepository.save(interaction);
    }
}