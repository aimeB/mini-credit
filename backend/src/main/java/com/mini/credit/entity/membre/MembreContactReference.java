package com.mini.credit.entity.membre;

import com.mini.credit.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "membre_contact_reference")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MembreContactReference extends BaseEntity {



    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "membre_id", nullable = false)
    private Membre membre;

    @Column(name = "nom_complet", nullable = false, length = 150)
    private String nomComplet;

    @Column(length = 100)
    private String lien;

    @Column(length = 30)
    private String telephone;

    private String adresse;
    private String observation;
}
