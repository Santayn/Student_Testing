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
import org.santayn.testing.models.user.User;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "`UserPermissions`")
public class UserPermission {

    @EmbeddedId
    private UserPermissionId id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "`UserId`", referencedColumnName = "`Id`", insertable = false, updatable = false)
    private User user;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "`PermissionId`", referencedColumnName = "`Id`", insertable = false, updatable = false)
    private Permission permission;

    public UserPermission(Integer userId, Integer permissionId) {
        this.id = new UserPermissionId(userId, permissionId);
    }
}
