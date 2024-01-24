package net.hostsharing.hsadminng.hs.office.person;

public enum HsOfficePersonType {
    UNKNOWN_PERSON_TYPE("??"),
    NATURAL_PERSON("NP"), // a human being
    LEGAL_PERSON("LP"), // incorporated legal entity like A/S, GmbH, e.K., eG, e.V.
    INCORPORATED_FIRM("IF"), // registered business partnership like OHG, Partnerschaftsgesellschaft
    UNINCORPORATED_FIRM("UF"), // unregistered partnership, association etc. like GbR, ARGE, community of heirs
    PUBLIC_INSTITUTION("PI"); // entities under public law like government entities, KdöR, AöR

    public final String shortName;

    HsOfficePersonType(final String shortName) {
        this.shortName = shortName;
    }

    @Override
    public String toString() {
        return shortName;
    }
}
