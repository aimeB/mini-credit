package com.mini.credit.entity.credit;

import com.mini.credit.entity.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "contrat_credit")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContratCredit extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "credit_id", nullable = false, unique = true)
    private Credit credit;

    @Column(name = "numero_contrat", nullable = false, unique = true, length = 50)
    private String numeroContrat;

    @Column(name = "date_signature", nullable = false)
    private LocalDate dateSignature;

    @Column(name = "lieu_signature", length = 150)
    private String lieuSignature;

    @Column(name = "objet_contrat", columnDefinition = "TEXT")
    private String objetContrat;

    @Column(name = "clauses_specifiques", columnDefinition = "TEXT")
    private String clausesSpecifiques;

    @Column(name = "fichier_url")
    private String fichierUrl;

    @Column(name = "signe_par_membre", nullable = false)
    private Boolean signeParMembre = false;

    @Column(name = "signe_par_institution", nullable = false)
    private Boolean signeParInstitution = false;

    @Column(name = "nom_signataire_institution", length = 150)
    private String nomSignataireInstitution;

    @Column(name = "fonction_signataire_institution", length = 150)
    private String fonctionSignataireInstitution;

    @OneToMany(mappedBy = "contratCredit", cascade = CascadeType.ALL)
    private List<PaiementCredit> paiements;
}