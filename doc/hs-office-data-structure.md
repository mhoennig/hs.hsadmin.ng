# Beispiel: juristische Person (GmbH) 

```mermaid
classDiagram
    direction TD

    namespace Hostsharing {
        class person-HostsharingEG
    }

    namespace Partner {
        class partner-MeierGmbH
        class role-MeierGmbH
        class personDetails-MeierGmbH
        class contactData-MeierGmbH
        class person-MeierGmbH
    }
    
    namespace Representatives {
        class person-FrankMeier
        class contactData-FrankMeier
        class role-MeierGmbH-FrankMeier
    }
    
    namespace Debitors {
        class debitor-MeierGmbH
        class contactData-MeierGmbH-Buha
        class role-MeierGmbH-Buha
    }

    namespace Operations {
        class person-SabineMeier
        class contactData-SabineMeier
        class role-MeierGmbH-SabineMeier
    }
    
    namespace Enums {

        class RoleType {
            <<enumeration>>
            UNKNOWN
            REPRESENTATIVE
            ACCOUNTING
            OPERATIONS
        }

        class PersonType {
            <<enumeration>>
            UNKNOWN: nur für Import
            NATURAL_PERSON: natürliche Person
            LEGAL_PERSON: z.B. GmbH, e.K., eG, e.V.
            INCORORATED_FIRM: z.B. OHG, Partnerschaftsgesellschaft
            UNINCORPORATED_FIRM: z.B. GbR, ARGE, Erbengemeinschaft
            PUBLIC_INSTITUTION: KdöR, AöR [ohne Registergericht/Registernummer]
        }
    }

    class person-HostsharingEG {
        +personType: LEGAL
        +tradeName: Hostsahring eG
        +familyName
        +givenName
    }

    class partner-MeierGmbH {
        +Numeric partnerNumber: 12345
        +Role partnerRole
    }
    partner-MeierGmbH *-- role-MeierGmbH

    class person-MeierGmbH {
        +personType: LEGAL
        +tradeName: Meier GmbH
        +familyName
        +givenName
    }
    person-MeierGmbH *-- personDetails-MeierGmbH

    class personDetails-MeierGmbH {
        +registrationOffice: AG Hamburg
        +registrationNumber: ABC123434
        +birthName
        +birthPlace
        +dateOfDeath
    }

    class contactData-MeierGmbH {
        +postalAddress:     Hauptstraße 5, 22345 Hamburg
        +phoneNumbers:      +49 40 12345-00
        +emailAddresses:    office@meier-gmbh.de
    }

    class role-MeierGmbH {
        +RoleType RoleType PARTNER
        +Person anchor
        +Person holder
        +Contact roleContact
    }
    role-MeierGmbH o-- person-HostsharingEG : anchor
    role-MeierGmbH o-- person-MeierGmbH : holder
    role-MeierGmbH o-- contactData-MeierGmbH

    %% --- Debitors ---

    class debitor-MeierGmbH {
        +Partner    partner
        +Numeric[2] debitorNumberSuffix:    00
        +Role       billingRole
        +boolean    billable:               true
        +String     vatId:                  ID123456789
        +String     vatCountryCode:         DE
        +boolean    vatBusiness:            true
        +boolean    vatReverseCharge:       false
        +BankAccount refundBankAccount
        +String     defaultPrefix:          mei
    }
    debitor-MeierGmbH o-- partner-MeierGmbH
    debitor-MeierGmbH *-- role-MeierGmbH-Buha

    class contactData-MeierGmbH-Buha {
        +postalAddress:     Hauptstraße 5, 22345 Hamburg
        +phoneNumbers:      +49 40 12345-05
        +emailAddresses:    buha@meier-gmbh.de
    }

    class role-MeierGmbH-Buha {
        +RoleType RoleType ACCOUNTING
        +Person anchor
        +Person holder
        +Contact roleContact
    }
    role-MeierGmbH-Buha o-- person-MeierGmbH : anchor
    role-MeierGmbH-Buha o-- person-MeierGmbH : holder
    role-MeierGmbH-Buha o-- contactData-MeierGmbH-Buha

    %% --- Representatives ---

    class person-FrankMeier {
        + personType: NATURAL
        + tradeName
        + familyName: Meier
        + givenName: Frank
    }

    class contactData-FrankMeier {
        +postalAddress
        +phoneNumbers: +49 40 12345-22
        +emailAddresses: frank.meier@meier-gmbh.de
    }

    class role-MeierGmbH-FrankMeier {
        +RoleType RoleType REPRESENTATIVE
        +Person anchor
        +Person holder
        +Contact roleContact
    }
    role-MeierGmbH-FrankMeier o-- person-MeierGmbH : anchor
    role-MeierGmbH-FrankMeier o-- person-FrankMeier : holder
    role-MeierGmbH-FrankMeier o-- contactData-FrankMeier

    %% --- Operations ---

    class person-SabineMeier {
        +personType: NATURAL
        +tradeName
        +familyName: Meier
        +givenName: Sabine
    }

    class contactData-SabineMeier {
        +postalAddress
        +phoneNumbers: +49 40 12345-22
        +emailAddresses: sabine.meier@meier-gmbh.de
    }

    class role-MeierGmbH-SabineMeier {
        +RoleType RoleType OPERATIONAL
        +Person anchor
        +Person holder
        +Contact roleContact
    }
    role-MeierGmbH-SabineMeier o-- person-MeierGmbH : anchor
    role-MeierGmbH-SabineMeier o-- person-SabineMeier : holder
    role-MeierGmbH-SabineMeier o-- contactData-SabineMeier

```
