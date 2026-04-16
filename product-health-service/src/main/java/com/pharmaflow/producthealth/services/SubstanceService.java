package com.pharmaflow.producthealth.services;

import com.pharmaflow.producthealth.dto.SubstanceDTO;
import com.pharmaflow.producthealth.exception.DuplicateResourceException;
import com.pharmaflow.producthealth.exception.ResourceNotFoundException;
import com.pharmaflow.producthealth.models.Substance;
import com.pharmaflow.producthealth.repositories.SubstanceRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SubstanceService {

    private final SubstanceRepository substanceRepository;
    private final ModelMapper modelMapper;

    @Transactional(readOnly = true)
    public List<SubstanceDTO> getAllSubstances() {
        return substanceRepository.findAll().stream().map(s -> modelMapper.map(s, SubstanceDTO.class)).toList();
    }

    @Transactional(readOnly = true)
    public SubstanceDTO getSubstanceById(Long id) {
        Substance substance = substanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supstanca sa ID " + id + " nije pronađena."));
        return modelMapper.map(substance, SubstanceDTO.class);
    }

    @Transactional
    public SubstanceDTO createSubstance(SubstanceDTO dto) {
        if (substanceRepository.existsByInn(dto.getInn()))
            throw new DuplicateResourceException("Supstanca sa INN '" + dto.getInn() + "' već postoji.");
        Substance substance = modelMapper.map(dto, Substance.class);
        substance.setId(null);
        return modelMapper.map(substanceRepository.save(substance), SubstanceDTO.class);
    }

    @Transactional
    public SubstanceDTO updateSubstance(Long id, SubstanceDTO dto) {
        Substance substance = substanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supstanca sa ID " + id + " nije pronađena."));
        if (!substance.getInn().equals(dto.getInn()) && substanceRepository.existsByInn(dto.getInn()))
            throw new DuplicateResourceException("Supstanca sa INN '" + dto.getInn() + "' već postoji.");
        substance.setInn(dto.getInn());
        substance.setCommonName(dto.getCommonName());
        substance.setDescription(dto.getDescription());
        substance.setAtcCode(dto.getAtcCode());
        return modelMapper.map(substanceRepository.save(substance), SubstanceDTO.class);
    }

    @Transactional
    public void deleteSubstance(Long id) {
        if (!substanceRepository.existsById(id))
            throw new ResourceNotFoundException("Supstanca sa ID " + id + " nije pronađena.");
        substanceRepository.deleteById(id);
    }
}
