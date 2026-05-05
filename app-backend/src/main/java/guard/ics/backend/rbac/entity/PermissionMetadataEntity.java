package guard.ics.backend.rbac.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Entity
@Table(name = "permission_metadata")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PermissionMetadataEntity {

    @EmbeddedId
    private PermissionMetadataId id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String value;

    @MapsId("permissionId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "permission_id")
    @JsonIgnore
    private PermissionEntity permission;

    @Embeddable
    @Getter @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class PermissionMetadataId implements Serializable {
        @Column(name = "permission_id")
        private Long permissionId;
        @Column(length = 128)
        private String key;
    }
}
