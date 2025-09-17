package net.hostsharing.hsadminng.rbac.subject;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.val;
import net.hostsharing.hsadminng.persistence.ImmutableBaseEntity;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@SuperBuilder(toBuilder = true)
public abstract class Subject<T extends Subject<?> & ImmutableBaseEntity<?>> implements ImmutableBaseEntity<T> {

    private static final int MAX_VALIDITY_DAYS = 21;
    private static final DateTimeFormatter DATE_FORMAT_WITH_FULLHOUR = DateTimeFormatter.ofPattern("MM-dd-yyyy HH");

    @Id
    @GeneratedValue
    private UUID uuid;

    private String name;

    public String generateAccessCode() {
        return generateAccessCode(LocalDateTime.now());
    }

    public boolean isValidAccessCode(final String accessCode, final int validityHours) {
        if (validityHours > 24 * MAX_VALIDITY_DAYS) {
            throw new IllegalArgumentException("Max validity (%s days) exceeded.".formatted(MAX_VALIDITY_DAYS));
        }
        if (generateAccessCode(LocalDateTime.now().minus(validityHours, ChronoUnit.HOURS)).equals(accessCode)) {
            return true;
        }
        if (validityHours < 0) {
            return false;
        }
        return isValidAccessCode(accessCode, validityHours - 1);
    }

    String generateAccessCode(final LocalDateTime timestamp) {
        val compound = name + ":" + uuid + ":" + timestamp.format(DATE_FORMAT_WITH_FULLHOUR);
        val code = String.valueOf(1000000 + Math.abs(compound.hashCode()) % 100000);
        return code.substring(1, 4) + ":" + code.substring(4, 7);
    }
}
