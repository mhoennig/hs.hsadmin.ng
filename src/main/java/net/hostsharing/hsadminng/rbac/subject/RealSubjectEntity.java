package net.hostsharing.hsadminng.rbac.subject;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.Immutable;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(schema = "rbac", name = "subject")
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@ToString(callSuper = true)
@Immutable
@NoArgsConstructor
@AttributeOverrides({
        @AttributeOverride(name = "uuid", column = @Column(name = "uuid"))
})
public class RealSubjectEntity extends Subject<RealSubjectEntity> {

}
