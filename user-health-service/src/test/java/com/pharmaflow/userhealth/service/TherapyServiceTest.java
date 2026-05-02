package com.pharmaflow.userhealth.service;
import com.pharmaflow.userhealth.dto.TherapyDTO;
import com.pharmaflow.userhealth.models.Therapy;
import com.pharmaflow.userhealth.repositories.TherapyRepository;
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
class TherapyServiceTest {
    @Mock private TherapyRepository therapyRepository;
    @Mock private ModelMapper modelMapper;
    @InjectMocks private TherapyService therapyService;
    @Test
    void getTherapyById_Success() {
        Therapy therapy = new Therapy();
        therapy.setId(1L);
        when(therapyRepository.findById(1L)).thenReturn(Optional.of(therapy));
        when(modelMapper.map(any(), eq(TherapyDTO.class))).thenReturn(new TherapyDTO());
        TherapyDTO result = therapyService.getTherapyById(1L);
        assertNotNull(result);
        verify(therapyRepository, times(1)).findById(1L);
    }
}