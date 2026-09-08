package com.mini.credit.repository;

import com.mini.credit.entity.agence.Agence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests d'intégration pour AgenceRepository
 * Utilise @SpringBootTest @ActiveProfiles("test") avec H2 (Flyway désactivé).
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("AgenceRepository Tests")
class AgenceRepositoryTest {

    @Autowired
    private AgenceRepository agenceRepository;

    @BeforeEach
    void setUp() {
        // Nettoyer la DB avant chaque test
        agenceRepository.deleteAll();
    }

    @Test
    @DisplayName("Devrait créer et retrouver une agence par code")
    void testFindByCodeAgence_WhenExists_ReturnsAgence() {
        // Arrange
        Agence testAgence = Agence.builder()
            .codeAgence("AGE-TEST")
            .nomAgence("Test Agence")
            .adresse("123 Test St")
            .telephone("+243123456789")
            .email("test@test.cd")
            .ville("Kinshasa")
            .actif(true)
            .description("Test Description")
            .build();
        agenceRepository.save(testAgence);

        // Act
        Optional<Agence> found = agenceRepository.findByCodeAgence("AGE-TEST");

        // Assert
        assertTrue(found.isPresent());
        assertEquals("Test Agence", found.get().getNomAgence());
        assertEquals("AGE-TEST", found.get().getCodeAgence());
    }

    @Test
    @DisplayName("Devrait retourner vide si agence n'existe pas")
    void testFindByCodeAgence_WhenNotExists_ReturnsEmpty() {
        // Act
        Optional<Agence> found = agenceRepository.findByCodeAgence("AGE-NOTFOUND");

        // Assert
        assertFalse(found.isPresent());
    }

    @Test
    @DisplayName("Devrait retrouver toutes les agences actives")
    void testFindByActifTrue_ReturnsOnlyActiveAgences() {
        // Arrange
        Agence agence1 = Agence.builder()
            .codeAgence("AGE-001")
            .nomAgence("Agence 1")
            .actif(true)
            .build();
        Agence agence2 = Agence.builder()
            .codeAgence("AGE-002")
            .nomAgence("Agence 2")
            .actif(false)
            .build();
        agenceRepository.save(agence1);
        agenceRepository.save(agence2);

        // Act
        List<Agence> activeAgences = agenceRepository.findByActifTrue();

        // Assert
        assertEquals(1, activeAgences.size());
        assertTrue(activeAgences.stream().anyMatch(a -> a.getCodeAgence().equals("AGE-001")));
    }

    @Test
    @DisplayName("Devrait vérifier l'existence d'une agence par code")
    void testExistsByCodeAgence_WhenExists_ReturnsTrue() {
        // Arrange
        Agence testAgence = Agence.builder()
            .codeAgence("AGE-TEST")
            .nomAgence("Test")
            .actif(true)
            .build();
        agenceRepository.save(testAgence);

        // Act & Assert
        assertTrue(agenceRepository.existsByCodeAgence("AGE-TEST"));
        assertFalse(agenceRepository.existsByCodeAgence("AGE-NOTFOUND"));
    }

    @Test
    @DisplayName("Devrait compter les agences actives")
    void testCountByActifTrue_ReturnsCorrectCount() {
        // Arrange
        agenceRepository.save(Agence.builder().codeAgence("AGE-001").nomAgence("A1").actif(true).build());
        agenceRepository.save(Agence.builder().codeAgence("AGE-002").nomAgence("A2").actif(true).build());
        agenceRepository.save(Agence.builder().codeAgence("AGE-003").nomAgence("A3").actif(false).build());

        // Act
        long count = agenceRepository.countByActifTrue();

        // Assert
        assertEquals(2, count);
    }
}
