package com.mini.credit.entity.caisse;

import com.mini.credit.entity.base.BaseEntity;
import com.mini.credit.entity.referentiel.Site;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id")
    private Site site;

    @Column(nullable = false, length = 10)
    private String devise = "CDF";

    @Column(nullable = false)
    private Boolean actif = true;
}
