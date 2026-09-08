package com.mini.credit.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO retourné lors de la création d'un membre
 * Contient le code unique d'activation au lieu du password
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivationCodeDTO {
    
    /**
     * Code unique: MBR-XXXXX-20260412
     */
    private String code;
    
    /**
     * Prénom du membre pour confirmation
     */
    private String memberName;
    
    /**
     * Numéro de téléphone
     */
    private String phoneNumber;
    
    /**
     * Message pour l'agent
     */
    private String message;

    /**
     * Lien d'activation public complet
     */
    private String activationLink;
    
    /**
     * Instructions d'activation
     */
    private String instructions;
    
    public static ActivationCodeDTO of(String code, String memberName, String phoneNumber, String activationLink) {
        return builder()
            .code(code)
            .memberName(memberName)
            .phoneNumber(phoneNumber)
            .message("✅ Membre créé avec succès! Code d'activation généré")
            .activationLink(activationLink)
            .instructions("📱 Agent: Communiquer ce code au membre (SMS, papier, oral)\n" +
                         "💬 Membre: Utiliser le lien d'activation ou saisir le code sur /activate\n" +
                         "🔐 Validité: 48 heures")
            .build();
    }
}
