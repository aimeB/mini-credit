package com.mini.credit.entity.referentiel;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Token d'activation unique pour les nouveaux membres
 * Remplace le système de password temporaire
 * - Un seul code par utilisateur actif
 * - Valide 48h après création
 * - Utilisable 1 fois uniquement
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "activation_tokens", indexes = {
    @Index(name = "idx_activation_code", columnList = "code"),
    @Index(name = "idx_activation_user", columnList = "utilisateur_id"),
    @Index(name = "idx_activation_used", columnList = "is_used")
})
public class ActivationToken {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Code unique: MBR-XXXXX-20260412 (12 caractères)
     */
    @Column(nullable = false, unique = true, length = 50)
    private String code;
    
    /**
     * Utilisateur associé
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private Utilisateur utilisateur;
    
    /**
     * Timestamp de création
     */
    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;
    
    /**
     * Deadline: code expire après 48h
     */
    @Column(name = "date_expiration", nullable = false)
    private LocalDateTime dateExpiration;
    
    /**
     * Flag: code a été utilisé
     */
    @Column(name = "is_used", nullable = false)
    @Builder.Default
    private Boolean isUsed = false;
    
    /**
     * Timestamp d'utilisation (quand l'utilisateur a activé son compte)
     */
    @Column(name = "date_utilisation")
    private LocalDateTime dateUtilisation;
    
    /**
     * IP de l'utilisateur qui a activé (audit)
     */
    @Column(name = "ip_activation", length = 45)
    private String ipActivation;
    
    @PrePersist
    protected void onCreate() {
        this.dateCreation = LocalDateTime.now();
        if (this.dateExpiration == null) {
            this.dateExpiration = LocalDateTime.now().plusHours(48);
        }
    }
    
    /**
     * Vérifier si le code a expiré
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.dateExpiration);
    }
    
    /**
     * Vérifier si le code est valide pour activation
     */
    public boolean isValidForActivation() {
        return !this.isUsed && !this.isExpired();
    }
}
