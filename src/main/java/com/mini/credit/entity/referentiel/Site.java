package com.mini.credit.entity.referentiel;


import com.mini.credit.entity.base.BaseEntity;
import com.mini.credit.entity.membre.Membre;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "site")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Site extends BaseEntity {



    @Column(name = "code_site", nullable = false, unique = true, length = 30)
    private String codeSite;

    @Column(name = "nom_site", nullable = false, length = 100)
    private String nomSite;

    private String adresse;
    private String commune;

    @Column(nullable = false, length = 100)
    private String ville = "Kinshasa";

    @Column(nullable = false)
    private Boolean actif = true;

    @OneToMany(mappedBy = "site")
    @Builder.Default
    private List<Membre> membres = new ArrayList<>();
}
