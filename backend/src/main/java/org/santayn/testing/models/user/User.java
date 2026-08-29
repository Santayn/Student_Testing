package org.santayn.testing.models.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.santayn.testing.models.person.Person;
import org.santayn.testing.models.role.Permission;
import org.santayn.testing.models.role.Role;

import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "`Users`")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "`Id`")
    private Integer id;

    @Column(name = "`Login`", nullable = false, length = 100, unique = true, columnDefinition = "citext")
    private String login;

    @Column(name = "`PasswordHash`", nullable = false, length = 500)
    @JsonIgnore
    private String passwordHash;

    @Column(name = "`IsActive`", nullable = false)
    private boolean active = true;

    @Column(name = "`PersonId`", unique = true)
    private Integer personId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "`PersonId`", referencedColumnName = "`Id`", insertable = false, updatable = false)
    private Person person;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "`UserRoles`",
            joinColumns = @JoinColumn(name = "`UserId`", referencedColumnName = "`Id`"),
            inverseJoinColumns = @JoinColumn(name = "`RoleId`", referencedColumnName = "`Id`")
    )
    private Set<Role> roles = new LinkedHashSet<>();

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "`UserPermissions`",
            joinColumns = @JoinColumn(name = "`UserId`", referencedColumnName = "`Id`"),
            inverseJoinColumns = @JoinColumn(name = "`PermissionId`", referencedColumnName = "`Id`")
    )
    private Set<Permission> permissions = new LinkedHashSet<>();

    @JsonIgnore
    public String getPassword() {
        return passwordHash;
    }

    public void setPassword(String password) {
        this.passwordHash = password;
    }

    public Role getRole() {
        return roles.stream().findFirst().orElse(null);
    }

    public void setRole(Role role) {
        roles.clear();
        if (role != null) {
            roles.add(role);
        }
    }

    public String getFirstName() {
        return person == null ? null : person.getFirstName();
    }

    public String getLastName() {
        return person == null ? null : person.getLastName();
    }

    public String getPhoneNumber() {
        return person == null ? null : person.getPhone();
    }

    public void setUsername(String username) {
        this.login = username;
    }
}
