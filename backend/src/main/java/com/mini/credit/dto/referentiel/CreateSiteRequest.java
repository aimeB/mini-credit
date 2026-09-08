package com.mini.credit.dto.referentiel;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateSiteRequest {

    @NotNull(message = "Agence requise")
    private Long agenceId;

    @NotBlank(message = "Code site requis")
    @Size(max = 30, message = "Code site max 30 caracteres")
    private String codeSite;

    @NotBlank(message = "Nom site requis")
    @Size(min = 2, max = 100, message = "Nom site: 2-100 caracteres")
    private String nomSite;

    /**
     * Zone operationnelle du site - champ obligatoire.
     * Ex : "De la Place Meteo jusqu a l avenue X"
     */
    @NotBlank(message = "Zone requise")
    @Size(max = 255, message = "Zone max 255 caracteres")
    private String zone;
}