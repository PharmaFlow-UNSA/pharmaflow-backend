package com.pharmaflow.userhealth.service;
import com.pharmaflow.userhealth.dto.FamilyMemberDTO;
import com.pharmaflow.userhealth.models.FamilyMember;
import com.pharmaflow.userhealth.models.User;
import com.pharmaflow.userhealth.models.enums.Relationship;
import com.pharmaflow.userhealth.repositories.FamilyMemberRepository;
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
class FamilyMemberServiceTest {
    @Mock private FamilyMemberRepository familyMemberRepository;
    @Mock private ModelMapper modelMapper;
    @InjectMocks private FamilyMemberService familyMemberService;
    @Test
    void getMemberById_Success() {
        User user = new User();
        user.setId(1L);
        FamilyMember member = new FamilyMember();
        member.setId(1L);
        member.setRelationship(Relationship.CHILD);
        member.setUser(user);
        when(familyMemberRepository.findById(1L)).thenReturn(Optional.of(member));
        FamilyMemberDTO result = familyMemberService.getFamilyMemberById(1L);
        assertNotNull(result);
        verify(familyMemberRepository, times(1)).findById(1L);
    }
}