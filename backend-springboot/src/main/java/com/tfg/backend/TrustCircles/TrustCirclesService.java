package com.tfg.backend.TrustCircles;

import com.tfg.backend.User.User;
import com.tfg.backend.User.UserRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TrustCirclesService {

	private final TrustCirclesRepository trustCirclesRepository;
	private final UserRepository userRepository;

	public TrustCirclesService(TrustCirclesRepository trustCirclesRepository, UserRepository userRepository) {
		this.trustCirclesRepository = trustCirclesRepository;
		this.userRepository = userRepository;
	}

	@Transactional(readOnly = true)
	public List<TrustCircles> list() {
		return trustCirclesRepository.findAllByDeletedAtIsNull();
	}

	@Transactional(readOnly = true)
	public TrustCircles getById(Long id) {
		return trustCirclesRepository.findByIdAndDeletedAtIsNull(id)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Círculo no encontrado"));
	}

	@Transactional(readOnly = true)
	public List<TrustCircles> getUserCircles(Long userId) {
		ensureUserExists(userId);
		return trustCirclesRepository.findActiveCirclesByUserId(userId);
	}

	@Transactional
	public TrustCircles createCircle(String name, List<Long> userIds) {
		if (name == null || name.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nombre del círculo es obligatorio");
		}

		if (trustCirclesRepository.existsByNameAndDeletedAtIsNull(name)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un círculo con ese nombre");
		}

		TrustCircles circle = new TrustCircles();
		circle.setName(name.trim());
		circle.setConsentDomain(false);

		if (userIds != null) {
			for (Long userId : userIds) {
				User user = ensureUserExists(userId);
				circle.addMember(user);
			}
		}

		return trustCirclesRepository.save(circle);
	}

	@Transactional
	public TrustCircles addUserToCircle(Long circleId, Long userId) {
		TrustCircles circle = getById(circleId);
		User user = ensureUserExists(userId);
		circle.addMember(user);
		return trustCirclesRepository.save(circle);
	}

	@Transactional
	public TrustCircles removeUserFromCircle(Long circleId, Long userId) {
		TrustCircles circle = getById(circleId);
		User user = ensureUserExists(userId);

		circle.removeMember(user);

		// Si era un dominio de consentimiento cruzado y deja de tener 2 miembros, se invalida.
		if (circle.isConsentDomain() && circle.getMembers().size() < 2) {
			circle.setDeletedAt(java.time.LocalDateTime.now());
		}

		return trustCirclesRepository.save(circle);
	}

	@Transactional
	public TrustCircles grantCrossCircleConsent(Long userId1, Long userId2) {
		if (userId1 == null || userId2 == null || userId1.equals(userId2)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Consentimiento cruzado inválido");
		}

		User user1 = ensureUserExists(userId1);
		User user2 = ensureUserExists(userId2);

		var existingDomain = trustCirclesRepository.findConsentDomainBetweenUsers(userId1, userId2);
		if (existingDomain.isPresent()) {
			return existingDomain.get();
		}

		TrustCircles consentDomain = new TrustCircles();
		consentDomain.setName(buildConsentCircleName(userId1, userId2));
		consentDomain.setConsentDomain(true);
		consentDomain.addMember(user1);
		consentDomain.addMember(user2);

		return trustCirclesRepository.save(consentDomain);
	}

	@Transactional(readOnly = true)
	public boolean canUsersCommunicate(Long userId1, Long userId2) {
		if (userId1 == null || userId2 == null || userId1.equals(userId2)) {
			return false;
		}

		return trustCirclesRepository.existsSharedActiveCircle(userId1, userId2);
	}

	@Transactional(readOnly = true)
	public void validateUsersCanCommunicate(Long userId1, Long userId2) {
		if (!canUsersCommunicate(userId1, userId2)) {
			throw new ResponseStatusException(
				HttpStatus.FORBIDDEN,
				"Los usuarios no comparten círculo de confianza ni consentimiento cruzado válido"
			);
		}
	}

	private User ensureUserExists(Long userId) {
		return userRepository.findByIdAndDeletedAtIsNull(userId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
	}

	private String buildConsentCircleName(Long userId1, Long userId2) {
		long low = Math.min(userId1, userId2);
		long high = Math.max(userId1, userId2);
		return "consent-" + low + "-" + high + "-" + System.currentTimeMillis();
	}
}
