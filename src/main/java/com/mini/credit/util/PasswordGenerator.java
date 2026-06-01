package com.mini.credit.util;

import java.security.SecureRandom;

/**
 * Utilité pour générer des mots de passe temporaires sécurisés
 */
public class PasswordGenerator {

    private static final String UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS = "0123456789";
    private static final String SPECIAL_CHARS = "!@#$%^&*()_+-=[]{}|;:,.<>?";
    private static final String ALL_CHARS = UPPERCASE + LOWERCASE + DIGITS + SPECIAL_CHARS;
    private static final SecureRandom random = new SecureRandom();

    /**
     * Génère un mot de passe temporaire aléatoire
     * Format: 12 caractères avec majuscules, minuscules, chiffres et caractères spéciaux
     *
     * @return Un mot de passe aléatoire sécurisé
     */
    public static String generateTemporaryPassword() {
        StringBuilder password = new StringBuilder(12);

        // Assurer que le mot de passe contient au minimum un caractère de chaque type
        password.append(UPPERCASE.charAt(random.nextInt(UPPERCASE.length())));
        password.append(LOWERCASE.charAt(random.nextInt(LOWERCASE.length())));
        password.append(DIGITS.charAt(random.nextInt(DIGITS.length())));
        password.append(SPECIAL_CHARS.charAt(random.nextInt(SPECIAL_CHARS.length())));

        // Remplir le reste avec des caractères aléatoires
        for (int i = 4; i < 12; i++) {
            password.append(ALL_CHARS.charAt(random.nextInt(ALL_CHARS.length())));
        }

        // Mélanger les caractères
        return shuffleString(password.toString());
    }

    private static String shuffleString(String input) {
        char[] chars = input.toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = chars[i];
            chars[i] = chars[j];
            chars[j] = temp;
        }
        return new String(chars);
    }

    /**
     * Génère un token de réinitialisation de mot de passe
     * Format: 64 caractères hexadécimaux
     *
     * @return Un token unique
     */
    public static String generateResetToken() {
        byte[] randomBytes = new byte[32];
        random.nextBytes(randomBytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : randomBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
