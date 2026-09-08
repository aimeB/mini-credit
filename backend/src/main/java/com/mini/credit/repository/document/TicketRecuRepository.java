package com.mini.credit.repository.document;

import com.mini.credit.entity.document.TicketRecu;
import com.mini.credit.enums.TypeTicketRecu;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TicketRecuRepository extends JpaRepository<TicketRecu, Long> {
    Optional<TicketRecu> findByCodeVerification(String codeVerification);
    Optional<TicketRecu> findByNumeroTicket(String numeroTicket);
    Optional<TicketRecu> findByOperationEpargneIdAndTypeTicketAndOriginalTicketIsNull(Long operationEpargneId, TypeTicketRecu typeTicket);
    List<TicketRecu> findByOperationEpargneIdOrderByDateGenerationDesc(Long operationEpargneId);
    List<TicketRecu> findByDemandeRetraitEpargneIdOrderByDateGenerationDesc(Long demandeRetraitEpargneId);
    List<TicketRecu> findByMembreIdOrderByDateGenerationDesc(Long membreId);
    boolean existsByNumeroTicket(String numeroTicket);
    boolean existsByCodeVerification(String codeVerification);
}
