package com.mini.credit.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Service pour gérer l'envoi d'emails
 */
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    /**
     * Envoie un email de bienvenue avec le mot de passe temporaire
     *
     * @param destinataire Email du destinataire
     * @param nom Nom complet de l'utilisateur
     * @param username Nom d'utilisateur
     * @param motDePasseTemporaire Mot de passe temporaire généré
     */
    public void envoyerEmailBienvenueMotDePasse(String destinataire, String nom, String username, String motDePasseTemporaire) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("noreply@minicredit.com");
            message.setTo(destinataire);
            message.setSubject("Bienvenue - Compte créé - Mini Crédit");
            message.setText(construireCorpsEmailBienvenue(nom, username, motDePasseTemporaire));

            mailSender.send(message);
            System.out.println("✓ Email de bienvenue envoyé à : " + destinataire);
        } catch (Exception e) {
            System.err.println("✗ Erreur lors de l'envoi de l'email : " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Envoie un email de réinitialisation de mot de passe
     *
     * @param destinataire Email du destinataire
     * @param nom Nom complet de l'utilisateur
     * @param lienReinitialisation Lien de réinitialisation
     */
    public void envoyerEmailReinitialisation(String destinataire, String nom, String lienReinitialisation) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("noreply@minicredit.com");
            message.setTo(destinataire);
            message.setSubject("Réinitialisation de mot de passe - Mini Crédit");
            message.setText(construireCorpsEmailReinitialisation(nom, lienReinitialisation));

            mailSender.send(message);
            System.out.println("✓ Email de réinitialisation envoyé à : " + destinataire);
        } catch (Exception e) {
            System.err.println("✗ Erreur lors de l'envoi de l'email de réinitialisation : " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String construireCorpsEmailBienvenue(String nom, String username, String motDePasse) {
        return "Bonjour " + nom + ",\n\n" +
                "Bienvenue sur la plateforme Mini Crédit !\n\n" +
                "Votre compte utilisateur a été créé avec les informations suivantes :\n" +
                "- Nom d'utilisateur : " + username + "\n" +
                "- Mot de passe temporaire : " + motDePasse + "\n\n" +
                "⚠️ IMPORTANT :\n" +
                "1. Connectez-vous avec ces identifiants\n" +
                "2. Vous serez invité à changer votre mot de passe à la première connexion\n" +
                "3. Choisissez un mot de passe fort et unique\n" +
                "4. Ne partagez jamais vos identifiants\n\n" +
                "Si vous n'avez pas demandé la création de ce compte, veuillez contacter l'administrateur immédiatement.\n\n" +
                "Cordialement,\n" +
                "L'équipe Mini Crédit";
    }

    private String construireCorpsEmailReinitialisation(String nom, String lienReinitialisation) {
        return "Bonjour " + nom + ",\n\n" +
                "Vous avez demandé une réinitialisation de votre mot de passe.\n\n" +
                "Cliquez sur le lien suivant pour réinitialiser votre mot de passe :\n" +
                lienReinitialisation + "\n\n" +
                "Ce lien expire dans 24 heures.\n\n" +
                "Si vous n'avez pas demandé cette réinitialisation, ignorez cet email.\n\n" +
                "Cordialement,\n" +
                "L'équipe Mini Crédit";
    }
}
