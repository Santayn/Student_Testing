package org.santayn.testing.models.role;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;


@Entity
@Table(name = "app_role")
public class Role {
    @Id
    Integer id;
    String Name;

}
