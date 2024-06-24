### HsHostingAssetEntity-Validation

There is just a single `HsHostingAssetEntity` class for all types of hosting assets like Managed-Server, Managed-Webspace, Unix-Users, Databases etc. These are distinguished by  `HsHostingAssetType HsHostingAssetEntity.type`.

For each of these types, a distinct validator has to be 
implemented as a subclass of `HsHostingAssetEntityValidator` which needs to be registered (see `HsHostingAssetEntityValidatorRegistry`) for the relevant type(s).

### Kinds of Validations

#### Identifier validation

The identifier of a Hosting-Asset is for example the Webspace-Name like "xyz00" or a Unix-User-Name like "xyz00-test".

To validate the identifier, vverride the method `identifierPattern(...)` and return a regular expression to validate the identifier against. The regular expression can depend on the actual entity instance.

#### Reference validation

References in this context are:
- the related Booking-Item,
- the parent-Hosting-Asset,
- the Assigned-To-Hosting-Asset and
- the Contact.

The first parameters of the `HsHostingAssetEntityValidator` superclass take rule descriptors for these references. These are all Subclasses fo   

### Validation Order

The validations are called in a sensible order. E.g. if a property value is not numeric, it makes no sense to check the total sum of such values to be within certain numeric values. And if the related booking item is of wrong type, it makes no sense to validate limits against sub-entities.

Properties are validated all at once, though. Thus, if multiple properties fail validation, all error messages are returned at once.

In general, the validation es executed in this order:

1. the entity itself
   1. its references
   2. its properties
2. the limits of the parent entity (parent asset + booking item)
3. limits against the own own-sub-entities

This implementation can be found in `HsHostingAssetEntityValidator.validate`.
