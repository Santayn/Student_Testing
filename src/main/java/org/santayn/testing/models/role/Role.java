package org.santayn.testing.models.role;

import jakarta.persistence.*;
import lombok.Data;


@Entity
@Table(name = "app_role")
@Data
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;
    String name;
}
