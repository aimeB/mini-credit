package com.mini.credit.service.impl;

import com.mini.credit.dto.epargne.CompteEpargneCreateRequest;
import com.mini.credit.dto.epargne.CompteEpargneResponse;
import com.mini.credit.entity.epargne.CompteEpargne;
import com.mini.credit.entity.membre.Membre;
import com.mini.credit.enums.StatutCompte;
import com.mini.credit.enums.StatutMembre;
import com.mini.credit.enums.TypeCompteEpargne;
import com.mini.credit.exception.BusinessException;
import com.mini.credit.mapper.SavingMapper;
import com.mini.credit.repository.epargne.CompteEpargneRepository;
import com.mini.credit.repository.membre.MembreRepository;
import com.mini.credit.service.ReferenceGeneratorService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompteEpargneServiceImplTest {

    @Mock private CompteEpargneRepository compteEpargneRepository;
    @Mock private MembreRepository membreRepository;
    @Mock private SavingMapper savingMapper;
    @Mock private ReferenceGeneratorService referenceGeneratorService;

    @InjectMocks private CompteEpargneServiceImpl service;

    @Test
    void create_shouldRefuseSecondActiveAccount() {
        Membre membre = new Membre();
        membre.setId(10L);
        membre.setStatut(StatutMembre.ACTIF);

        when(membreRepository.findById(10L)).thenReturn(Optional.of(membre));
        when(compteEpargneRepository.existsByMembreIdAndStatut(10L, StatutCompte.ACTIF)).thenReturn(true);

        CompteEpargneCreateRequest request = new CompteEpargneCreateRequest();
        request.setMembreId(10L);
        request.setTypeCompte(TypeCompteEpargne.COTISATION);
        request.setDateOuverture(LocalDate.now());

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("déjà un compte épargne actif");
    }

    @Test
    void createMissingForMember_shouldCreateActiveAccountWhenMissing() {
        Membre membre = new Membre();
        membre.setId(12L);
        membre.setStatut(StatutMembre.ACTIF);

        when(membreRepository.findById(12L)).thenReturn(Optional.of(membre));
        when(compteEpargneRepository.existsByMembreIdAndStatut(12L, StatutCompte.ACTIF)).thenReturn(false);
        when(referenceGeneratorService.genererReference("CEP")).thenReturn("CEP202606150010");
        when(compteEpargneRepository.save(any(CompteEpargne.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(savingMapper.toResponse(any(CompteEpargne.class))).thenReturn(CompteEpargneResponse.builder().id(88L).membreId(12L).build());

        service.createMissingForMember(12L);

        ArgumentCaptor<CompteEpargne> captor = ArgumentCaptor.forClass(CompteEpargne.class);
        verify(compteEpargneRepository).save(captor.capture());
        CompteEpargne saved = captor.getValue();

        assertThat(saved.getMembre().getId()).isEqualTo(12L);
        assertThat(saved.getStatut()).isEqualTo(StatutCompte.ACTIF);
        assertThat(saved.getTypeCompte()).isEqualTo(TypeCompteEpargne.EPARGNE_VOLONTAIRE);
        assertThat(saved.getSoldeDisponible()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(saved.getSoldeBloque()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void createMissingForMember_shouldRefuseWhenActiveAccountExists() {
        Membre membre = new Membre();
        membre.setId(13L);
        membre.setStatut(StatutMembre.ACTIF);

        when(membreRepository.findById(13L)).thenReturn(Optional.of(membre));
        when(compteEpargneRepository.existsByMembreIdAndStatut(13L, StatutCompte.ACTIF)).thenReturn(true);

        assertThatThrownBy(() -> service.createMissingForMember(13L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("déjà un compte épargne actif");
    }
}
