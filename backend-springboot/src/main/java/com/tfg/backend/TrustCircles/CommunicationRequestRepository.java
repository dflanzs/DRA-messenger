package com.tfg.backend.TrustCircles;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommunicationRequestRepository extends JpaRepository<CommunicationRequest, Long> {

    /**
     * Busca una solicitud con el estado dado entre dos usuarios, en cualquier
     * dirección (userA -> userB o userB -> userA).
     */
    @Query("SELECT r FROM CommunicationRequest r WHERE r.status = :status "
        + "AND ((r.requester.id = :userA AND r.target.id = :userB) "
        + "OR (r.requester.id = :userB AND r.target.id = :userA))")
    Optional<CommunicationRequest> findBetweenUsersWithStatus(
        @Param("userA") Long userA,
        @Param("userB") Long userB,
        @Param("status") CommunicationRequestStatus status);

    List<CommunicationRequest> findByTargetIdAndStatus(Long targetId, CommunicationRequestStatus status);

    List<CommunicationRequest> findByRequesterIdAndStatus(Long requesterId, CommunicationRequestStatus status);
}
