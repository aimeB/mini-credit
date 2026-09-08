package com.mini.credit.entity.caisse;

import com.mini.credit.entity.base.BaseEntity;
import com.mini.credit.entity.agence.Agence;
import com.mini.credit.entity.referentiel.Site;
import com.mini.credit.entity.referentiel.Utilisateur;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "caisse")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Caisse extends BaseEntity {



    @Column(name = "code_caisse", nullable = false, unique = true, length = 30)
    private String codeCaisse;

    @Column(nullable = false, length = 100)
    private String libelle;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agence_id", nullable = false)
    private Agence agence;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id")
    private Site site;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caissier_responsable_id")
    private Utilisateur caissierResponsable;

    @Column(nullable = false, length = 10)
    private String devise = "CDF";

    @Column(nullable = false)
    private Boolean actif = true;
}
