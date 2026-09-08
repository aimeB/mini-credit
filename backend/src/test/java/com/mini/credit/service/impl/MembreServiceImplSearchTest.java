package com.mini.credit.service.impl;

import com.mini.credit.dto.membre.MembreResponse;
import com.mini.credit.entity.referentiel.AgentTerrain;
import com.mini.credit.entity.referentiel.Role;
import com.mini.credit.entity.referentiel.Site;
import com.mini.credit.entity.referentiel.Utilisateur;
import com.mini.credit.enums.security.RoleCode;
import com.mini.credit.mapper.MembreMapper;
import com.mini.credit.repository.agentTerrain.AgentTerrainRepository;
import com.mini.credit.repository.membre.MembreRepository;
import com.mini.credit.repository.referentiel.SiteRepository;
import com.mini.credit.service.ActivationService;
import com.mini.credit.service.UtilisateurService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MembreServiceImplSearchTest {

    @Mock private MembreRepository membreRepository;
    @Mock private SiteRepository siteRepository;
    @Mock private AgentTerrainRepository agentTerrainRepository;
    @Mock private MembreMapper membreMapper;
    @Mock private UtilisateurService utilisateurService;
    @Mock private ActivationService activationService;

    @InjectMocks private MembreServiceImpl service;

    private Utilisateur agentUser;

    @BeforeEach
    void setup() {
        Role role = new Role();
        role.setCode(RoleCode.AGENT_TERRAIN);

        agentUser = new Utilisateur();
        agentUser.setId(10L);
        agentUser.setUsername("agent01");
        agentUser.setRole(role);

        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(agentUser, null, List.of())
        );
    }

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void searchAgentTerrainForceSiteScope() {
        Site site = new Site();
        site.setId(77L);

        AgentTerrain agent = new AgentTerrain();
        agent.setId(1L);
        agent.setSite(site);

        when(agentTerrainRepository.findByUtilisateurId(10L)).thenReturn(Optional.of(agent));
        when(membreRepository.searchActive(eq("marie"), eq(77L), any())).thenReturn(new PageImpl<>(List.of()));

        var result = service.search("marie", 999L, PageRequest.of(0, 20));
        assertThat(result.getTotalElements()).isEqualTo(0);
        verify(membreRepository, times(1)).searchActive(eq("marie"), eq(77L), any());
    }

    @Test
    void searchRetournePage() {
        Role adminRole = new Role();
        adminRole.setCode(RoleCode.ADMIN);
        Utilisateur admin = new Utilisateur();
        admin.setId(99L);
        admin.setRole(adminRole);

        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(admin, null, List.of())
        );

        var membreResponse = MembreResponse.builder().id(1L).codeMembre("MB001").nomComplet("Marie Test").build();

        when(membreRepository.searchActive(eq("MB001"), eq(5L), any())).thenReturn(new PageImpl<>(List.of(new com.mini.credit.entity.membre.Membre())));
        when(membreMapper.toResponse(any())).thenReturn(membreResponse);

        var page = service.search("MB001", 5L, PageRequest.of(0, 10));
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getCodeMembre()).isEqualTo("MB001");
    }

    @Test
    void getAllPage_adminRetourneActifsViaSearchActive() {
        Role adminRole = new Role();
        adminRole.setCode(RoleCode.ADMIN);
        Utilisateur admin = new Utilisateur();
        admin.setId(99L);
        admin.setRole(adminRole);

        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(admin, null, List.of())
        );

        when(membreRepository.searchActive(eq(null), eq(null), any())).thenReturn(new PageImpl<>(List.of(new com.mini.credit.entity.membre.Membre())));
        when(membreMapper.toResponse(any())).thenReturn(MembreResponse.builder().id(1L).build());

        var page = service.getAll(PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
        verify(membreRepository).searchActive(eq(null), eq(null), any());
    }

    @Test
    void getAllPage_agentTerrainForceSiteScope() {
        Site site = new Site();
        site.setId(77L);

        AgentTerrain agent = new AgentTerrain();
        agent.setId(1L);
        agent.setSite(site);

        when(agentTerrainRepository.findByUtilisateurId(10L)).thenReturn(Optional.of(agent));
        when(membreRepository.searchActive(eq(null), eq(77L), any())).thenReturn(new PageImpl<>(List.of()));

        var page = service.getAll(PageRequest.of(0, 20));

        assertThat(page.getTotalElements()).isEqualTo(0);
        verify(membreRepository).searchActive(eq(null), eq(77L), any());
    }
}
