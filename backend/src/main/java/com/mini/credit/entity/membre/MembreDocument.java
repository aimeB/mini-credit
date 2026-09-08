package com.mini.credit.entity.membre;

import com.mini.credit.entity.base.BaseEntity;
import com.mini.credit.enums.TypeDocumentMembre;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "membre_document")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MembreDocument extends BaseEntity {



    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "membre_id", nullable = false)
    private Membre membre;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_document", nullable = false, length = 30)
    private TypeDocumentMembre typeDocument;

    @Column(name = "numero_document", length = 100)
    private String numeroDocument;

    @Column(name = "fichier_url")
    private String fichierUrl;

    @Column(name = "date_expiration")
    private LocalDate dateExpiration;

    private String observation;
}
