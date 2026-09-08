package com.mini.credit.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PHASE 6B.1: Unit tests for StatutFicheJournaliere enum
 */
@DisplayName("StatutFicheJournaliere Tests")
class StatutFicheJournaliereTest {

    @Test
    @DisplayName("Should have all required values")
    void testEnumValues() {
        assertNotNull(StatutFicheJournaliere.BROUILLON);
        assertNotNull(StatutFicheJournaliere.SOUMISE);
        assertNotNull(StatutFicheJournaliere.VALIDEE);
        assertNotNull(StatutFicheJournaliere.REJETEE);
        assertNotNull(StatutFicheJournaliere.ANNULEE);
    }

    @Test
    @DisplayName("Should have descriptions")
    void testDescriptions() {
        assertTrue(StatutFicheJournaliere.BROUILLON.getDescription().contains("encodées"));
        assertTrue(StatutFicheJournaliere.SOUMISE.getDescription().contains("Consolidation"));
        assertTrue(StatutFicheJournaliere.VALIDEE.getDescription().contains("Validée"));
        assertTrue(StatutFicheJournaliere.REJETEE.getDescription().contains("Rejetée"));
        assertTrue(StatutFicheJournaliere.ANNULEE.getDescription().contains("Annulée"));
    }

    @Test
    @DisplayName("Should identify terminal states")
    void testIsTerminal() {
        assertFalse(StatutFicheJournaliere.BROUILLON.isTerminal());
        assertFalse(StatutFicheJournaliere.SOUMISE.isTerminal());
        assertFalse(StatutFicheJournaliere.VALIDEE.isTerminal());
        assertFalse(StatutFicheJournaliere.REJETEE.isTerminal());
        assertTrue(StatutFicheJournaliere.ANNULEE.isTerminal());
    }

    @Test
    @DisplayName("Should allow BROUILLON to transition to SOUMISE")
    void testBrouillonToSoumise() {
        assertTrue(StatutFicheJournaliere.BROUILLON.canTransitionTo(StatutFicheJournaliere.SOUMISE));
    }

    @Test
    @DisplayName("Should allow BROUILLON to transition to ANNULEE")
    void testBrouillonToAnnulee() {
        assertTrue(StatutFicheJournaliere.BROUILLON.canTransitionTo(StatutFicheJournaliere.ANNULEE));
    }

    @Test
    @DisplayName("Should deny BROUILLON to VALIDEE")
    void testBrouillonToValidee() {
        assertFalse(StatutFicheJournaliere.BROUILLON.canTransitionTo(StatutFicheJournaliere.VALIDEE));
    }

    @Test
    @DisplayName("Should allow SOUMISE to transition to VALIDEE")
    void testSoumiseToValidee() {
        assertTrue(StatutFicheJournaliere.SOUMISE.canTransitionTo(StatutFicheJournaliere.VALIDEE));
    }

    @Test
    @DisplayName("Should allow SOUMISE to transition to REJETEE")
    void testSoumiseToRejetee() {
        assertTrue(StatutFicheJournaliere.SOUMISE.canTransitionTo(StatutFicheJournaliere.REJETEE));
    }

    @Test
    @DisplayName("Should allow SOUMISE to transition to ANNULEE")
    void testSoumiseToAnnulee() {
        assertTrue(StatutFicheJournaliere.SOUMISE.canTransitionTo(StatutFicheJournaliere.ANNULEE));
    }

    @Test
    @DisplayName("Should allow VALIDEE to transition to ANNULEE")
    void testValideeToAnnulee() {
        assertTrue(StatutFicheJournaliere.VALIDEE.canTransitionTo(StatutFicheJournaliere.ANNULEE));
    }

    @Test
    @DisplayName("Should deny VALIDEE to REJETEE")
    void testValideeToRejetee() {
        assertFalse(StatutFicheJournaliere.VALIDEE.canTransitionTo(StatutFicheJournaliere.REJETEE));
    }

    @Test
    @DisplayName("Should allow REJETEE to transition back to BROUILLON")
    void testRejeteeToBrouillon() {
        assertTrue(StatutFicheJournaliere.REJETEE.canTransitionTo(StatutFicheJournaliere.BROUILLON));
    }

    @Test
    @DisplayName("Should allow REJETEE to transition to ANNULEE")
    void testRejeteToAnnulee() {
        assertTrue(StatutFicheJournaliere.REJETEE.canTransitionTo(StatutFicheJournaliere.ANNULEE));
    }

    @Test
    @DisplayName("Should deny ANNULEE any transitions")
    void testAnnuleeNoTransitions() {
        assertFalse(StatutFicheJournaliere.ANNULEE.canTransitionTo(StatutFicheJournaliere.BROUILLON));
        assertFalse(StatutFicheJournaliere.ANNULEE.canTransitionTo(StatutFicheJournaliere.SOUMISE));
        assertFalse(StatutFicheJournaliere.ANNULEE.canTransitionTo(StatutFicheJournaliere.VALIDEE));
        assertFalse(StatutFicheJournaliere.ANNULEE.canTransitionTo(StatutFicheJournaliere.REJETEE));
        assertFalse(StatutFicheJournaliere.ANNULEE.canTransitionTo(StatutFicheJournaliere.ANNULEE));
    }

    @Test
    @DisplayName("Should allow same state transition")
    void testSameStateTransition() {
        assertTrue(StatutFicheJournaliere.BROUILLON.canTransitionTo(StatutFicheJournaliere.BROUILLON));
        assertTrue(StatutFicheJournaliere.SOUMISE.canTransitionTo(StatutFicheJournaliere.SOUMISE));
    }

    @Test
    @DisplayName("Should handle null target")
    void testNullTarget() {
        assertFalse(StatutFicheJournaliere.BROUILLON.canTransitionTo(null));
    }
}
