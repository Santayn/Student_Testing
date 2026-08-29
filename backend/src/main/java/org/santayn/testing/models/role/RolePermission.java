package org.santayn.testing.models.role;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "`RolePermissions`")
public class RolePermission {

    @EmbeddedId
    private RolePermissionId id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "`RoleId`", referencedColumnName = "`Id`", insertable = false, updatable = false)
    private Role role;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "`PermissionId`", referencedColumnName = "`Id`", insertable = false, updatable = false)
    private Permission permission;

    public RolePermission(Integer roleId, Integer permissionId) {
        this.id = new RolePermissionId(roleId, permissionId);
    }
}
