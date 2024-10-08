# Handling Automatic Creation of Hosting Assets for New Booking Items

**Status:**
- [x] proposed by (Michael Hönnig)
- [ ] accepted by (Participants)
- [ ] rejected by (Participants)
- [ ] superseded by (superseding ADR)


## Context and Problem Statement

When a customer creates a new booking item (e.g., `MANAGED_WEBSPACE`), the system must automatically create the related hosting asset.
This process can sometimes fail or require additional data from the user, e.g. installing a DNS verification key, or a hostmaster, e.g. the target server to use.

The challenge is how to handle this automatic creation process while dealing with missing data, asynchronicity and failures while ensuring system consistency and proper user notification.


### Technical Background

The creation of hosting assets can occur synchronously (in simple cases) or asynchronously (when additional steps like manual verification are needed).
For example, a `DOMAIN_SETUP` hosting asset may require DNS verification from the user, and until this is provided, the related domain cannot be fully set up.

Additionally, not all data needed for creating the hosting asset is stored in the booking item.
It's part of the HTTP request and later stored in the hosting asset, but we also need to store it before the hosting asset can be created asynchronously.

Current system behavior involves returning HTTP 201 upon booking item creation, but the automatic hosting asset creation might fail due to missing information.
The system needs to manage the creation process in a way that ensures valid hosting assets are created and informs the user of any actions required while still returning a 201 HTTP code, not an error code.


## Considered Options

For storing the data needed for the hosting-asset creation:

* STORAGE-1: Store temporary asset data in the `BookingItemEntity`, e.g. a JSON column.
    And delete the value of that column, once the hosting assets got successfully created.
* STORAGE-2: Create hosting assets immediately, even if invalid, but mark them as "inactive" until completed and fully validated.
* STORAGE-3: Store the asset data in a kind of event- or job-queue, which get deleted once the hosting-asset got successfully created.

For the user-notification status:

* STATUS-1: Introduce a status field in the booking-items.
* STATUS-2: Store the status in the event-/job-queue entries.

### STORAGE-1: Temporary Data Storage in `BookingItemEntity`

Store asset-related data (e.g., domain name) in a temporary column or JSON field in the `BookingItemEntity` until the hosting assets are successfully created.
Once assets are created, the temporary data is deleted to avoid inconsistencies.

#### Advantages
- Easy to implement.

#### Disadvantages
- Needs either a separate map of properties in the booking-item.
- Or, if stored as a JSON field in the booking-item-resources, these are misused. 
- Requires additional cleanup logic to remove stale data.

### STORAGE-2: Inactive Hosting Assets Until Validation

Create the hosting assets immediately upon booking item creation but mark them as "inactive" until all required information (e.g., verification code) is provided and validation is complete.

#### Advantages
- Avoids temporary external data storage for the hosting-assets.

#### Disadvantages
- Validation becomes more complex as some properties need to be validated, others not.
    And some properties even need special treatment for new entities, which then becomes vague.
- Inactive assets have to be filtered from operational assets.
- Potential risk of incomplete or inconsistent assets being created, which may require correction.
- Difficult to write tests for all possible combinations of validations.

### STORAGE-3: Event-Based Approach

The hosting asset data required for creation us passed to the API and stored in a `BookingItemCreatedEvent`.
If hosting asset creation cannot happen synchronously, the event is stored and processed asynchronously in batches, retrying failed asset creation as needed.

#### Advantages
- Clean-data-structure (separation of concerns).
- Clear separation between booking item creation and hosting asset creation.
- Only valid assets in the database.
- Can handle complex asynchronous processes (like waiting for external verification) in a clean and structured manner.
- Easier to manage retries and failures in asset creation without complicating the booking item structure.

#### Disadvantages
- At the Spring controller level, the whole JSON is already converted into Java objects,
    but for storing the asset data in the even, we need JSON again.
    This could is not just a performance-overhead but could also lead to inconsistencies.

### STATUS-1: Store hosting-asset-creation-status in the `BookingItemEntity`

A status field would be added to booking-items to track the creation state of related hosting assets.
The users could check their booking-items for the status of the hosting-asset creation, error messages and further instructions.

#### Advantages
- Easy to implement.

#### Disadvantages
- Adds a field to the booking-item which is makes no sense anymore once the related hosting asset is created.


### Status-2: Store hosting-asset-creation-status in the `BookingItemCreateEvent`

A status field would be added to the booking-item-created event and get updated with the latest messages any time we try to create the hosting-asset. 

#### Advantages
- Clean-data-structure (separation of concerns)

#### Disadvantages
- Accessing the status requires querying the event queue.


## Decision Outcome

**Chosen Option: STORAGE-3 with STATUS-2 (Event-Based Approach with `BookingItemCreatedEvent`)**

The event-based approach was selected as the best solution for handling automatic hosting asset creation. This option provides a clear separation between booking item creation and hosting asset creation, ensuring that no invalid or incomplete assets are created. The asynchronous nature of the event system allows for retries and external validation steps (such as user-entered verification codes) without disrupting the overall flow.

By using `BookingItemCreatedEvent` to store the hosting-asset data and the status,
we don't need to misuse other data structures for temporary data
and therefore hava a clean separation of concerns.
