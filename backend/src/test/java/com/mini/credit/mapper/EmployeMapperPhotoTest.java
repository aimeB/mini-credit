package com.mini.credit.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mini.credit.dto.employe.CreateEmployeRequest;
import com.mini.credit.dto.employe.EmployeDTO;
import com.mini.credit.dto.employe.UpdateEmployeRequest;
import com.mini.credit.entity.employe.Employe;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmployeMapperPhotoTest {

    private final EmployeMapper mapper = new EmployeMapper();

    @Test
    void toDtoExposeLaPhoto() {
        Employe employe = new Employe();
        employe.setPhotoUrl("/uploads/employes/1.jpg");

        EmployeDTO result = mapper.toDTO(employe);

        assertThat(result.getPhotoUrl()).isEqualTo("/uploads/employes/1.jpg");
    }

    @Test
    void toEntityConserveUnePhotoValide() {
        CreateEmployeRequest request = CreateEmployeRequest.builder()
                .nom("Terrain")
                .prenom("Agent")
                .photoUrl("https://cdn.example/employe-1.jpg")
                .build();

        Employe result = mapper.toEntity(request);

        assertThat(result.getPhotoUrl()).isEqualTo("https://cdn.example/employe-1.jpg");
    }

    @Test
    void updatePhotoRemplaceEtVideLaPhotoSurDemandeExplicite() {
        Employe employe = new Employe();
        employe.setPhotoUrl("/uploads/employes/ancienne.jpg");
        UpdateEmployeRequest request = new UpdateEmployeRequest();

        request.setPhotoUrl("https://cdn.example/nouvelle.jpg");
        mapper.updateEntityFromDTO(request, employe);
        assertThat(employe.getPhotoUrl()).isEqualTo("https://cdn.example/nouvelle.jpg");

        request.setPhotoUrl("");
        mapper.updateEntityFromDTO(request, employe);
        assertThat(employe.getPhotoUrl()).isNull();
    }

    @Test
    void refuseUnePhotoDangereuse() {
        UpdateEmployeRequest request = new UpdateEmployeRequest();
        request.setPhotoUrl("javascript:alert(1)");

        assertThatThrownBy(() -> mapper.updateEntityFromDTO(request, new Employe()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("photoUrl");
    }

    @Test
    void deserialisePhotoNullSansErreurEtMarqueLeChampCommeFourni() throws Exception {
        UpdateEmployeRequest request = new ObjectMapper()
                .readValue("{\"photoUrl\":null}", UpdateEmployeRequest.class);

        assertThat(request.getPhotoUrl()).isNull();
        assertThat(request.isPhotoUrlProvided()).isTrue();
    }
}
