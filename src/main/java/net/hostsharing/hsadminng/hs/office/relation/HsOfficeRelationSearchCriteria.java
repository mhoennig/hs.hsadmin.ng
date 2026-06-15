package net.hostsharing.hsadminng.hs.office.relation;

import lombok.Builder;
import java.util.UUID;

/**
 * Search criteria for finding relations by a conjunction of optional parameters.
 *
 * @param personUuid       optional UUID of the anchor person or holder person
 * @param anchorPersonUuid optional UUID of the anchor person
 * @param holderPersonUuid optional UUID of the holder person
 * @param relationType     type of the relation
 * @param mark             the mark (supports {@code %} as wildcard), case-insensitive
 * @param personData       string to match against the person's {@code tradeName}, {@code familyName} or {@code givenName}
 *                         (supports {@code %} as wildcard), case-insensitive
 * @param contactData      string to match against the contact's {@code caption}, {@code postalAddress},
 *                         {@code emailAddresses} or {@code phoneNumbers} (supports {@code %} as wildcard), case-insensitive
 * @param contactUuid      optional UUID of the contact
 */
@Builder
public record HsOfficeRelationSearchCriteria(
        UUID personUuid,
        UUID anchorPersonUuid,
        UUID holderPersonUuid,
        HsOfficeRelationType relationType,
        String mark,
        String personData,
        String contactData,
        UUID contactUuid
) {
    public String getRelationTypeString() {
        return relationType == null ? null : relationType.name();
    }

    public String getPersonDataPattern() {
        return toSqlLikeOperand(personData);
    }

    public String getMarkPattern() {
        return toSqlLikeOperand(mark);
    }

    public String getContactDataPattern() {
        return toSqlLikeOperand(contactData);
    }

    private static String toSqlLikeOperand(final String text) {
        return text == null ? null : ("%" + text.toLowerCase() + "%");
    }
}
