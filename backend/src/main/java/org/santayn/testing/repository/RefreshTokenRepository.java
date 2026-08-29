package org.santayn.testing.repository;

import org.santayn.testing.models.user.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Integer> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findByUserIdAndRevokedAtUtcIsNullAndExpiresAtUtcAfter(Integer userId, Instant now);
}
