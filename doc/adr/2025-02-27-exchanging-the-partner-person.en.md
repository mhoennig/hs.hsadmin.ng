# Changing a Business Partner or Invoice Recipient (Debitor)

**Status:**
- [x] Proposed by (Michael Hönnig)
- [ ] Accepted by (...)
- [ ] Rejected by (...)
- [ ] Replaced by (replacing ADR)

## Context and Problem Statement

In the given data model of business partners and invoice recipients (debitors), which also includes business roles such as representative, technical contacts, or mailing list subscriptions, the question arises of how to efficiently and consistently implement a change of the business partner person. These business roles are each linked to the partner person.

A concrete example is changing from a natural person who has passed away to their heir community.
**It has been shown that handling the API is complex and error-prone due to the large number of newly created objects and their links. Additionally, not all changes can be carried out in a single transaction, which can lead to inconsistencies.**

The following elements must be updated:

1. All relations with the old partner person:
    - The PARTNER relation
    - The DEBITOR relations (possibly multiple)
    - The OPERATIONS relations (possibly multiple)
    - The SUBSCRIBER relations (possibly multiple)
    - The REPRESENTATIVE relations (possibly multiple)
    - etc.
2. The PARTNER relation has the peculiarity that it is referenced by the partner and therefore must also be replaced there.
3. The DEBITOR relation has the peculiarity that it is referenced by the debitor and therefore must also be replaced there.

As a result, as many of these *rewirings* as possible should be done in the backend.
A central point is needed to trigger this cascade.

Currently, there are three possible approaches to implementing this change dynamically, each with different impacts on effort, API, and access rights.

### Technical Background

At the time of this ADR's creation, the following relevant entities exist:
- **Person**: A natural or legal entity (name, company, salutation, etc.)
- **Contact**: Contact data of a business role
- **Relation**: A relationship from one person (Holder) to another (Anchor), with a type (e.g., PARTNER, DEBITOR, REPRESENTATIVE) and contact data
- **Partner**: Essentially additional data of a PARTNER relation (currently only the partner number), linking a partner person to the Hostsharing person
- **Debitor**: Essentially additional data of a DEBITOR relation, linking a debitor person to a partner person

Access rights are managed through a hierarchical, dynamic RBAC system, where the **OWNER** of an entity instance has all rights, **ADMIN** can update defined fields, **AGENT** can create links, and **TENANT**, **GUEST**, and **REFERRER** have read-only access.
Partners and debitors use the RBAC roles of the associated relations.

## Considered Alternatives

* **1. Replace Relations:** Replace PARTNER/DEBITOR/OPERATIONS/... relations with a new relation for the new partner person (e.g., heir community) as the new Holder via PATCH on /api/hs/office/partners/UUID
* **2. Directly Update Relations:** Change the Holder reference in the existing PARTNER relation to the new partner person (e.g., heir community) via PATCH on /api/hs/office/relations/UUID
* **3. Update Relations via Partner:** Change the partner person in the PARTNER relation via PATCH on /api/hs/office/partners/UUID

### Option 1: Replace Relations

The exchange of the partner (and debitor) person is done by creating a new PARTNER or DEBITOR relation, and then updating the reference in the partner or debitor to point to the new relation instead of the old one.

#### Advantages

- **Preserving the API:** This behavior is already implemented and requires no major API remodelling, only an extension to swap additional relations.
- **UPDATE permission for AGENT:** The AGENT role of a relation could be granted UPDATE rights because only the non-critical contact reference would be modifiable.
- **Congruence of business logic and API:** Conceptually, this aligns with replacing the partner person, though technically, a new PARTNER relation is created instead of directly replacing the person.

#### Disadvantages

- **Loss of explicit GRANTs:** Explicit GRANTs on the PARTNER relation would be lost due to the relation being replaced. Preserving these would require additional implementation effort.
- **Mismatch between business logic and API:** The exchange of the partner person would not directly occur at the partner but rather through a new PARTNER relation.
- **Not applicable to dependent relations:** Updating dependent relations (e.g., representatives, operational contacts, billing contacts, mailing list subscriptions) would require creating new relations and deleting old ones, again leading to the loss of explicit GRANTs.
- **Performance issues with many dependent relations:** Dependent relations can only be exchanged via loops rather than direct SQL UPDATEs, leading to poorer performance.

### Option 2: Directly Update Relations

The existing PARTNER relation remains unchanged, and the Holder is switched from the deceased person to the heir community.

#### Advantages

- **Applicability to both partner and debitor persons:** This approach would work for both partner and debitor persons at a generic level.
- **API uniformity and generality:** REST API changes would belong uniformly to the relation endpoint, consistent with how contact changes are currently handled.

#### Disadvantages

- **UPDATE permission for relation-AGENT would be problematic:** The relation-AGENT must not have permission to swap the Holder. Since there are no column-specific update rights, they would also lose the ability to change the Contact.
- **API remodelling:** The exchange of a partner person would move from the partner endpoint (/api/hs/office/partner) to the relation endpoint, requiring significant restructuring, including tests.
- **Mismatch between business logic and API:** Although conceptually it involves replacing a partner person, technically, the change would occur at the PARTNER relation.

### Option 3: Update Relations via Partner

The partner (or debitor) person would still be updated at the partner or debitor level, but instead of creating a new relation, the reference to the person would be updated in the existing PARTNER (or DEBITOR) relation. Dependent relations could be updated efficiently via SQL UPDATE.

#### Advantages

- **Preserving the API:** The endpoint /api/hs/office/partners/UUID remains unchanged, requiring only internal adjustments.
- **UPDATE permission for AGENT:** AGENT roles could be granted UPDATE rights, while API controls could limit modifications.
- **Congruence of business logic and API:** The technical implementation matches the conceptual model of replacing a partner person.

#### Disadvantages

No significant drawbacks were identified, other than allowing UPDATE permissions on relations while controlling updates at the API level.

## Decision and Outcome

**Decision:** 3. Update Relations via Partner

**Rationale:**
- The API accurately reflects the business logic (PATCH partner person on /api/hs/office/partners/UUID)
- The effort required is relatively low (many updates can be done via SQL UPDATEs)
- UPDATE permission can be granted to relation-AGENT without security risks (since the API controls access)

  | Criteria \ Relations ...                       | 1. Replace | 2. Directly Update | 3. Update via Partner |
  |------------------------------------------------|-----------:|-------------------:|----------------------:|
  | **Technical and Effort Criteria**              |            |                    |                       |
  | Preserve API vs. Remodelling (incl. risk)      |         +2 |                 -2 |                    +1 |
  | Applicability to Partner and Debitor Person    |            |                 +1 |                       |
  | Applicability to dependent relations           |         -3 |                    |                       |
  | Performance with many dependent relations      |         -1 |                    |                       |
  | Effort for explicit grants                     |         -1 |                    |                       |
  | **Intermediate Score**                         |     **-3** |             **-1** |                **+1** |
  |                                                |            |                    |                       |
  | **Business Criteria**                          |            |                    |                       |
  | Congruence of Business Logic and API           |         +1 |                 -1 |                    +1 |
  | Uniformity/Generality of the API               |            |                 +1 |                       |
  | UPDATE Permission for Relation-AGENT possible  |         +1 |                    |                    +1 |
  | **Intermediate Score**                         |     **+2** |              **0** |                **+2** |
  |                                                |            |                    |                       |
  | **Final Score**                                |     **-1** |             **-1** |                **+3** |


