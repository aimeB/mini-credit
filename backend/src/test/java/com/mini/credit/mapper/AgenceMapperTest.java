package com.mini.credit.mapper;

import com.mini.credit.dto.AgenceDTO;
import com.mini.credit.dto.CreateAgenceRequest;
import com.mini.credit.dto.UpdateAgenceRequest;
import com.mini.credit.entity.agence.Agence;
import com.mini.credit.repository.EmployeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires pour AgenceMapper
 */
@DisplayName("AgenceMapper Tests")
class AgenceMapperTest {

    private AgenceMapper mapper;
    
    @Mock
    private EmployeRepository employeRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mapper = new AgenceMapper(employeRepository);
    }

    @Test
    @DisplayName("Devrait convertir entité Agence en DTO")
    void testToDTO_WithValidEntity_ReturnsDTO() {
        // Arrange
        LocalDateTime now = LocalDateTime.now();
        Agence agence = Agence.builder()
            .codeAgence("AGE-001")
            .nomAgence("Test Agence")
            .adresse("123 Test St")
            .commune("Gombe")
            .quartier("Centre")
            .reference("Ref-001")
            .telephone("+243123456789")
            .email("test@test.cd")
            .ville("Kinshasa")
            .actif(true)
            .description("Test Description")
            .build();
        agence.setId(1L);
        agence.setDateCreation(now);
        agence.setDateModification(now);

        // Act
        AgenceDTO dto = mapper.toDTO(agence);

        // Assert
        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("AGE-001", dto.getCodeAgence());
        assertEquals("Test Agence", dto.getNomAgence());
        assertEquals("123 Test St", dto.getAdresse());
        assertEquals("Gombe", dto.getCommune());
        assertEquals("Centre", dto.getQuartier());
        assertEquals("Ref-001", dto.getReference());
        assertEquals("+243123456789", dto.getTelephone());
        assertEquals("test@test.cd", dto.getEmail());
        assertEquals("Kinshasa", dto.getVille());
        assertTrue(dto.getActif());
        assertEquals("Test Description", dto.getDescription());
    }

    @Test
    @DisplayName("Devrait retourner null si entité est null")
    void testToDTO_WithNullEntity_ReturnsNull() {
        // Act
        AgenceDTO dto = mapper.toDTO(null);

        // Assert
        assertNull(dto);
    }

    @Test
    @DisplayName("Devrait convertir CreateAgenceRequest en entité Agence")
    void testToEntity_WithValidCreateRequest_ReturnsEntity() {
        // Arrange
        CreateAgenceRequest request = CreateAgenceRequest.builder()
            .codeAgence("age-001") // minuscule
            .nomAgence("Test Agence")
            .adresse("123 Test St")
            .commune("Gombe")
            .quartier("Centre")
            .reference("Ref-001")
            .telephone("+243123456789")
            .ville("Kinshasa")
            .actif(true)
            .description("Test Description")
            .build();

        // Act
        Agence entity = mapper.toEntity(request);

        // Assert
        assertNotNull(entity);
        assertEquals("AGE-001", entity.getCodeAgence()); // Converti en majuscules
        assertEquals("Test Agence", entity.getNomAgence());
        assertEquals("123 Test St", entity.getAdresse());
        assertEquals("Gombe", entity.getCommune());
        assertEquals("Centre", entity.getQuartier());
        assertEquals("Ref-001", entity.getReference());
        assertEquals("+243123456789", entity.getTelephone());
        assertNull(entity.getEmail()); // email n'est plus dans le flux principal
        assertEquals("Kinshasa", entity.getVille());
        assertTrue(entity.getActif());
        assertEquals("Test Description", entity.getDescription());
    }

    @Test
    @DisplayName("Devrait convertir code en majuscules lors de la conversion")
    void testToEntity_ConvertCodeToUpperCase() {
        // Arrange
        CreateAgenceRequest request = CreateAgenceRequest.builder()
            .codeAgence("age-test")
            .nomAgence("Test")
            .actif(true)
            .build();

        // Act
        Agence entity = mapper.toEntity(request);

        // Assert
        assertEquals("AGE-TEST", entity.getCodeAgence());
    }

    @Test
    @DisplayName("Devrait retourner null si CreateRequest est null")
    void testToEntity_WithNullCreateRequest_ReturnsNull() {
        // Act
        Agence entity = mapper.toEntity((CreateAgenceRequest) null);

        // Assert
        assertNull(entity);
    }

    @Test
    @DisplayName("Devrait mettre à jour entité à partir d'UpdateRequest")
    void testUpdateEntityFromDTO_WithValidRequest_UpdatesEntity() {
        // Arrange
        Agence entity = Agence.builder()
            .codeAgence("AGE-001")
            .nomAgence("Old Name")
            .adresse("Old Address")
            .commune("Old Commune")
            .quartier("Old Quartier")
            .telephone("Old Phone")
            .email("old@test.cd")
            .ville("Old City")
            .actif(true)
            .description("Old Description")
            .build();
        entity.setId(1L);

        UpdateAgenceRequest request = UpdateAgenceRequest.builder()
            .nomAgence("New Name")
            .adresse("New Address")
            .commune("New Commune")
            .quartier("New Quartier")
            .reference("Ref-New")
            .telephone("New Phone")
            .ville("New City")
            .actif(false)
            .description("New Description")
            .build();

        // Act
        mapper.updateEntityFromDTO(request, entity);

        // Assert
        assertEquals(1L, entity.getId()); // ID ne change pas
        assertEquals("AGE-001", entity.getCodeAgence()); // Code ne change pas
        assertEquals("New Name", entity.getNomAgence());
        assertEquals("New Address", entity.getAdresse());
        assertEquals("New Commune", entity.getCommune());
        assertEquals("New Quartier", entity.getQuartier());
        assertEquals("Ref-New", entity.getReference());
        assertEquals("New Phone", entity.getTelephone());
        assertEquals("old@test.cd", entity.getEmail()); // email non modifié par update
        assertEquals("New City", entity.getVille());
        assertFalse(entity.getActif());
        assertEquals("New Description", entity.getDescription());
    }

    @Test
    @DisplayName("Devrait ignorer null request lors de la mise à jour")
    void testUpdateEntityFromDTO_WithNullRequest_NoChanges() {
        // Arrange
        Agence entity = Agence.builder()
            .nomAgence("Original Name")
            .build();
        entity.setId(1L);

        // Act
        mapper.updateEntityFromDTO(null, entity);

        // Assert
        assertEquals("Original Name", entity.getNomAgence());
    }

    @Test
    @DisplayName("Devrait ignorer null entity lors de la mise à jour")
    void testUpdateEntityFromDTO_WithNullEntity_NoException() {
        // Arrange
        UpdateAgenceRequest request = UpdateAgenceRequest.builder()
            .nomAgence("New Name")
            .build();

        // Act & Assert - ne devrait pas lancer d'exception
        assertDoesNotThrow(() -> mapper.updateEntityFromDTO(request, null));
    }

    @Test
    @DisplayName("Devrait convertir DTO en entité avec valeurs partielles")
    void testToEntity_WithPartialRequest_ReturnsEntityWithDefaults() {
        // Arrange
        CreateAgenceRequest request = CreateAgenceRequest.builder()
            .codeAgence("AGE-PARTIAL")
            .nomAgence("Partial Agence")
            .actif(true)
            // Autres champs null/non définis
            .build();

        // Act
        Agence entity = mapper.toEntity(request);

        // Assert
        assertNotNull(entity);
        assertEquals("AGE-PARTIAL", entity.getCodeAgence());
        assertEquals("Partial Agence", entity.getNomAgence());
        assertTrue(entity.getActif());
        assertNull(entity.getAdresse());
        assertNull(entity.getCommune());
        assertNull(entity.getQuartier());
        assertNull(entity.getReference());
        assertNull(entity.getTelephone());
        assertNull(entity.getEmail());
    }

    @Test
    @DisplayName("Devrait préserver l'ID et timestamps lors de la conversion DTO")
    void testToDTO_PreservesMetadata() {
        // Arrange
        LocalDateTime creation = LocalDateTime.of(2026, 1, 1, 10, 0);
        LocalDateTime modification = LocalDateTime.of(2026, 1, 2, 10, 0);
        
        Agence agence = Agence.builder()
            .codeAgence("AGE-999")
            .nomAgence("Test")
            .actif(true)
            .build();
        agence.setId(999L);
        agence.setDateCreation(creation);
        agence.setDateModification(modification);

        // Act
        AgenceDTO dto = mapper.toDTO(agence);

        // Assert
        assertEquals(999L, dto.getId());
        assertEquals(creation, dto.getDateCreation());
        assertEquals(modification, dto.getDateModification());
    }
}
