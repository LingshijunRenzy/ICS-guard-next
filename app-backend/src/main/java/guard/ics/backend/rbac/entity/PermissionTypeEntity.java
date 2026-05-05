package guard.ics.backend.rbac.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "permission_types")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PermissionTypeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String name;

    @Column(length = 256)
    private String description;
}
