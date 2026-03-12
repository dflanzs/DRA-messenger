package com.tfg.backend.TrustCircles;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TrustCirclesRepository extends JpaRepository<TrustCircles, Long> {
	List<TrustCircles> findAllByDeletedAtIsNull();

	Optional<TrustCircles> findByIdAndDeletedAtIsNull(Long id);

	boolean existsByNameAndDeletedAtIsNull(String name);

	@Query("""
		SELECT CASE WHEN COUNT(tc) > 0 THEN true ELSE false END
		FROM TrustCircles tc
		JOIN tc.members m1
		JOIN tc.members m2
		WHERE tc.deletedAt IS NULL
		  AND m1.id = :userId1
		  AND m2.id = :userId2
	""")
	boolean existsSharedActiveCircle(@Param("userId1") Long userId1, @Param("userId2") Long userId2);

	@Query("""
		SELECT DISTINCT tc
		FROM TrustCircles tc
		JOIN tc.members m
		WHERE tc.deletedAt IS NULL
		  AND m.id = :userId
	""")
	List<TrustCircles> findActiveCirclesByUserId(@Param("userId") Long userId);

	@Query("""
		SELECT DISTINCT tc
		FROM TrustCircles tc
		JOIN tc.members m1
		JOIN tc.members m2
		WHERE tc.deletedAt IS NULL
		  AND tc.consentDomain = true
		  AND m1.id = :userId1
		  AND m2.id = :userId2
		  AND SIZE(tc.members) = 2
	""")
	Optional<TrustCircles> findConsentDomainBetweenUsers(
		@Param("userId1") Long userId1,
		@Param("userId2") Long userId2
	);
}
