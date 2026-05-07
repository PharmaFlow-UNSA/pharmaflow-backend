package com.pharmaflow.userhealth.service;
import com.pharmaflow.userhealth.dto.PatientProfileDTO;
import com.pharmaflow.userhealth.models.PatientProfile;
import com.pharmaflow.userhealth.models.enums.BloodType;
import com.pharmaflow.userhealth.repositories.PatientProfileRepository;
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
class PatientProfileServiceTest {
    @Mock private PatientProfileRepository patientProfileRepository;
    @Mock private ModelMapper modelMapper;
    @InjectMocks private PatientProfileService patientProfileService;
    @Test
    void getProfileById_Success() {
        PatientProfile profile = new PatientProfile();
        profile.setId(1L);
        profile.setBloodType(BloodType.A_POSITIVE);
        when(patientProfileRepository.findById(1L)).thenReturn(Optional.of(profile));
        when(modelMapper.map(any(), eq(PatientProfileDTO.class))).thenReturn(new PatientProfileDTO());
        PatientProfileDTO result = patientProfileService.getPatientProfileById(1L);
        assertNotNull(result);
        verify(patientProfileRepository, times(1)).findById(1L);
    }
}