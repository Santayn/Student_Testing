package org.santayn.testing.models.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "`RefreshTokens`")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "`Id`")
    private Integer id;

    @Column(name = "`UserId`", nullable = false)
    private Integer userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "`UserId`", referencedColumnName = "`Id`", insertable = false, updatable = false)
    private User user;

    @Column(name = "`TokenHash`", nullable = false, length = 128, unique = true)
    private String tokenHash;

    @Column(name = "`LifetimeKind`", nullable = false)
    private int lifetimeKind = 1;

    @Column(name = "`CreatedAtUtc`", nullable = false)
    private Instant createdAtUtc;

    @Column(name = "`ExpiresAtUtc`", nullable = false)
    private Instant expiresAtUtc;

    @Column(name = "`CreatedByIp`", length = 45)
    private String createdByIp;

    @Column(name = "`CreatedByUserAgent`", length = 1024)
    private String createdByUserAgent;

    @Column(name = "`RevokedAtUtc`")
    private Instant revokedAtUtc;

    @Column(name = "`RevokedByIp`", length = 45)
    private String revokedByIp;

    @Column(name = "`ReplacedByTokenHash`", length = 128)
    private String replacedByTokenHash;

    public boolean isActive() {
        return revokedAtUtc == null && expiresAtUtc != null && expiresAtUtc.isAfter(Instant.now());
    }
}
