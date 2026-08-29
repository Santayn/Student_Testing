package org.santayn.testing.models.role;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Embeddable
public class UserPermissionId implements Serializable {

    @Column(name = "`UserId`", nullable = false)
    private Integer userId;

    @Column(name = "`PermissionId`", nullable = false)
    private Integer permissionId;
}
