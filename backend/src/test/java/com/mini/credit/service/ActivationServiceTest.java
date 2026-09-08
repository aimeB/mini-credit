package com.mini.credit.service;

import com.mini.credit.dto.auth.ActivationCodeDTO;
import com.mini.credit.entity.referentiel.ActivationToken;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.exception.BusinessException;
import com.mini.credit.repository.ActivationTokenRepository;
import com.mini.credit.repository.UtilisateurRepository;
import com.mini.credit.security.JwtProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ActivationService - génération du lien d'activation")
class ActivationServiceTest {

    @Mock
    private ActivationTokenRepository activationTokenRepository;

    @Mock
    private UtilisateurRepository utilisateurRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtProvider jwtProvider;

    @InjectMocks
    private ActivationService activationService;

    @Test
    @DisplayName("generateActivationCode_shouldUseConfiguredFrontendPublicUrl")
    void generateActivationCode_shouldUseConfiguredFrontendPublicUrl() {
        Utilisateur utilisateur = buildUtilisateur();
        ReflectionTestUtils.setField(activationService, "frontendPublicUrl", "https://app.mini-credit.cd");
        when(activationTokenRepository.findActiveTokenByUtilisateur(utilisateur)).thenReturn(Optional.empty());
        when(activationTokenRepository.existsByCodeAndIsUsedFalse(anyString())).thenReturn(false);
        when(activationTokenRepository.save(any(ActivationToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ActivationCodeDTO dto = activationService.generateActivationCode(utilisateur);

        assertThat(dto.getActivationLink()).startsWith("https://app.mini-credit.cd/activate?code=");
        assertThat(dto.getActivationLink()).contains(dto.getCode());
        assertThat(dto.getActivationLink()).doesNotContain("192.168.");
        assertThat(dto.getInstructions()).contains("48 heures");
    }

    @Test
    @DisplayName("generateActivationCode_shouldTrimTrailingSlash")
    void generateActivationCode_shouldTrimTrailingSlash() {
        Utilisateur utilisateur = buildUtilisateur();
        ReflectionTestUtils.setField(activationService, "frontendPublicUrl", "https://URL_OFFICIELLE_APPLICATION/");
        when(activationTokenRepository.findActiveTokenByUtilisateur(utilisateur)).thenReturn(Optional.empty());
        when(activationTokenRepository.existsByCodeAndIsUsedFalse(anyString())).thenReturn(false);
        when(activationTokenRepository.save(any(ActivationToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ActivationCodeDTO dto = activationService.generateActivationCode(utilisateur);

        assertThat(dto.getActivationLink()).isEqualTo("https://URL_OFFICIELLE_APPLICATION/activate?code=" + dto.getCode());
    }

    @Test
    @DisplayName("generateActivationCode_shouldFailWhenFrontendPublicUrlMissing")
    void generateActivationCode_shouldFailWhenFrontendPublicUrlMissing() {
        Utilisateur utilisateur = buildUtilisateur();
        ReflectionTestUtils.setField(activationService, "frontendPublicUrl", "   ");
        when(activationTokenRepository.findActiveTokenByUtilisateur(utilisateur)).thenReturn(Optional.empty());
        when(activationTokenRepository.existsByCodeAndIsUsedFalse(anyString())).thenReturn(false);

        assertThatThrownBy(() -> activationService.generateActivationCode(utilisateur))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("app.frontend-public-url")
                .hasMessageContaining("FRONTEND_PUBLIC_URL");
    }

    private Utilisateur buildUtilisateur() {
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setUsername("member01");
        utilisateur.setNomComplet("Membre Test");
        utilisateur.setEmail("member@test.cd");
        return utilisateur;
    }
}