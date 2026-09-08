package com.mini.credit.controller;

import com.mini.credit.dto.membre.MembreResponse;
import com.mini.credit.service.MembreService;
import com.mini.credit.service.security.ScopeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MembreController API")
class MembreControllerApiTest {

    @Mock
    private MembreService membreService;

    @Mock
    private ScopeService scopeService;

    @InjectMocks
    private MembreController controller;

    @Test
    void search_shouldReturnPage_andForwardParams() {
        when(membreService.search(eq("marie"), eq(10L), any()))
            .thenReturn(new PageImpl<>(List.of(MembreResponse.builder().id(1L).codeMembre("MB001").nomComplet("Marie Test").build()), PageRequest.of(0, 20), 1));

        var response = controller.search("marie", 10L, 0, 20);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getCodeMembre()).isEqualTo("MB001");
        verify(membreService).search(eq("marie"), eq(10L), any());
    }

    @Test
    void search_byCode_shouldReturnData() {
        when(membreService.search(eq("MB001"), eq(null), any()))
            .thenReturn(new PageImpl<>(List.of(MembreResponse.builder().id(2L).codeMembre("MB001").build()), PageRequest.of(0, 20), 1));

        var response = controller.search("MB001", null, 0, 20);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getId()).isEqualTo(2L);
    }

    @Test
    void getAll_shouldRequestPageZeroByDefault() {
        when(membreService.getAll(any())).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        controller.getAll(0, 10);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(membreService).getAll(pageableCaptor.capture());

        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isEqualTo(0);
        assertThat(pageable.getPageSize()).isEqualTo(10);
    }
}
