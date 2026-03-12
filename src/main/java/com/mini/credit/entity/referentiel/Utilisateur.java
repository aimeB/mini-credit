package com.mini.credit.entity.referentiel;


import com.mini.credit.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "utilisateur")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Utilisateur extends BaseEntity {


    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(name = "mot_de_passe_hash", nullable = false, columnDefinition = "TEXT")
    private String motDePasseHash;

    @Column(name = "nom_complet", nullable = false, length = 150)
    private String nomComplet;

    private String telephone;
    private String email;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id")
    private Site site;

    @Column(nullable = false)
    private Boolean actif = true;

    @Column(name = "derniere_connexion")
    private LocalDateTime derniereConnexion;
}
