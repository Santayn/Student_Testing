package org.santayn.testing.models.role;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "`Roles`")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "`Id`")
    private Integer id;

    @Column(name = "`Name`", nullable = false, length = 100, unique = true, columnDefinition = "citext")
    private String name;

    @Column(name = "`Description`", length = 500)
    private String description;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "`RolePermissions`",
            joinColumns = @JoinColumn(name = "`RoleId`", referencedColumnName = "`Id`"),
            inverseJoinColumns = @JoinColumn(name = "`PermissionId`", referencedColumnName = "`Id`")
    )
    private Set<Permission> permissions = new LinkedHashSet<>();
}
