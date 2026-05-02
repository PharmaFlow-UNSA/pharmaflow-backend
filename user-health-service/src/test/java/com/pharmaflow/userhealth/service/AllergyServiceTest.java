package com.pharmaflow.userhealth.service;
import com.pharmaflow.userhealth.dto.AllergyDTO;
import com.pharmaflow.userhealth.models.Allergy;
import com.pharmaflow.userhealth.models.enums.Severity;
import com.pharmaflow.userhealth.repositories.AllergyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class AllergyServiceTest {
    @Mock private AllergyRepository allergyRepository;
    @Mock private ModelMapper modelMapper;
    @InjectMocks private AllergyService allergyService;
    @Test
    void getAllergyById_Success() {
        Allergy allergy = new Allergy();
        allergy.setId(1L);
        allergy.setSeverity(Severity.HIGH);
        when(allergyRepository.findById(1L)).thenReturn(Optional.of(allergy));
        when(modelMapper.map(any(), eq(AllergyDTO.class))).thenReturn(new AllergyDTO());
        AllergyDTO result = allergyService.getAllergyById(1L);
        assertNotNull(result);
        verify(allergyRepository, times(1)).findById(1L);
    }
}