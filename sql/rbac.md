## *hsadmin-ng*'s Role-Based-Access-Management (RBAC)

The requirements of *hsadmin-ng* include table-m row- and column-level-security for read and write access to business-objects.
More precisely, any access has to be controlled according to given rules depending on the accessing users, their roles and the accessed business-object.
Further, roles and business-objects are hierarchical.

To avoid misunderstandings, we are using the term "business-object" what's usually called a "domain-object".
But as we are in the context of a webhosting infrastructure provider, "domain" would have a double meaning.

Our implementation is based on Role-Based-Access-Management (RBAC) in conjunction with views and triggers on the business-objects.
As far as possible, we are using the same terms as defined in the RBAC standard, for our function names though, we chose more expressive names.

In RBAC, subjects can be assigned to roles, roles can be hierarchical and eventually have assigned permissions.
A permission allows a specific operation (e.g. view or edit) on a specific (business-) object.

You can find the entity structure as a UML class diagram as follows:

```plantuml
@startuml
' left to right direction
top to bottom direction

' hide the ugly E in a circle left to the entity name
hide circle

' use right-angled line routing
skinparam linetype ortho

package RBAC {

    ' forward declarations    
    entity RbacUser
    entity RbacObject
    
    together {
        
        entity RbacRole
        entity RbacPermission
        enum RbacOperation
        
        RbacUser -[hidden]> RbacRole
        RbacRole -[hidden]> RbacUser
    }
    
    together {
        entity RbacGrant
        enum RbacReferenceType
        entity RbacReference
    }
    RbacReference -[hidden]> RbacReferenceType
    
    entity RbacGrant {
        ascendantUuid: uuid(RbackReference)
        descendantUuid: uuid(RbackReference)
        auto
    }
    RbacGrant o-> RbacReference
    RbacGrant o-> RbacReference
    
    enum RbacReferenceType {
        RbacUser
        RbacRole
        RbacPermission
    }
    RbacReferenceType ..> RbacUser
    RbacReferenceType ..> RbacRole
    RbacReferenceType ..> RbacPermission
    
    entity RbacReference {
        *uuid : uuid <<generated>>
        --
        type : RbacReferenceType
    }
    RbacReference o--> RbacReferenceType  
    entity RbacUser {
        *uuid : uuid <<generated>>
        --
        name : varchar
    }
    RbacUser o-- RbacReference
    
    entity RbacRole {
        *uuid : uuid(RbacReference)
        --
        name : varchar
    }
    RbacRole o-- RbacReference
    
    entity RbacPermission {
        *uuid : uuid(RbacReference)
        --
        objectUuid: RbacObject
        op: RbacOperation
    }
    RbacPermission o-- RbacReference
    RbacPermission o-- RbacOperation
    RbacPermission *-- RbacObject
    
    enum RbacOperation {
        add-package
        add-domain
        add-unixuser
        ...
        view
        edit
        delete
    }
    
    entity RbacObject {
        *uuid : uuid <<generated>>
        --
        objectTable: varchar
    }
    RbacObject o- "Business Objects"    
}

package "Business Objects" {

    entity package
    package *--u- RbacObject
    
    entity customer
    customer *--u- RbacObject

    entity "..." as moreBusinessObjects
    moreBusinessObjects *-u- RbacObject
}

@enduml
```

### The RBAC Entity Types

#### RbacReference

An *RbacReference* is a generalization of all entity types which participate in the hierarchical role system, defined via *RbacGrant*.

The primary key of the *RbacReference* and its referred object is always identical.

#### RbacUser

An *RbacUser* is a type of RBAC-subject which references a login account outside this system, identified by a name (usually an email-address).

*RbacUser*s can be assigned to multiple *RbacRole*s, through which they can get permissions to *RbacObject*s.

The primary key of the *RbacUser* is identical to its related *RbacReference*.

#### RbacRole

An *RbacRole* represents a collection of directly or indirectly assigned *RbacPermission*s. 
Each *RbacRole* can be assigned to *RbacUser*s or to another *RbacRole*.

Both kinds of assignments are represented via *RbacGrant*.

*RbacRole* entities can *RbacObject*s, or more precise

#### RbacPermission

An *RbacPermission* allows a specific *RbacOperation* on a specific *RbacObject*.

#### RbacOperation

An *RbacOperation* determines, <u>what</u> an *RbacPermission* allows to do.
It can be one of:

- **add-...** - permits creating new instances of specific entity types underneath the object specified by the permission, e.g. "add-package"
- **view** - permits reading the contents of the object specified by the permission
- **edit** - change the contents of the object specified by the permission
- **delete** - delete the object specified by the permission

This list is extensible according to the needs of the access rule system.

Please notice, that there is no **create-...** operation to create new instances of related business-object-types.
For such a singleton business-object-type, e.g. *Organization" or "Hostsharing" has to be defined, and its single entity is referred in the permission.
By this, the foreign key in *RbacPermission* can be defined as `NOT NULL`. 

#### RbacGrant

The *RbacGrant* entities represent the access-rights structure from *RbacUser*s via hierarchical *RbacRoles* down to *RbacPermission*s.

The core SQL queries to determine access rights are all recursive queries on the *RbacGrant* table.

### Role naming

Automatically generated roles are named as such:

#### business-table#business-object-name.tenant
This role is assigned to users who manage objects underneath the object which is accessible through the role.
This rule usually gets only view permissions assigned.

**Example**

'dd'

## Example Users, Roles, Permissions and Business-Objects 

```plantuml
@startuml
' left to right direction
top to bottom direction

' hide the ugly E in a circle left to the entity name
hide circle

' use right-angled line routing
' skinparam linetype ortho

package RbacUsers {
    object UserMike
    object UserSuse
    object UserPaul
}

package RbacRoles {
    object RoleAdministrators
    object RoleCustXyz_Owner
    object RoleCustXyz_Admin
    object RolePackXyz00_Owner
}
RbacUsers -[hidden]> RbacRoles

package RbacPermissions {
    object PermCustXyz_View
    object PermCustXyz_Edit
    object PermCustXyz_Delete
    object PermCustXyz_AddPackage
    object PermPackXyz00_View
    object PermPackXyz00_Edit
    object PermPackXyz00_Delete
    object PermPackXyz00_AddUser
}
RbacRoles -[hidden]> RbacPermissions

package BusinessObjects {
    object CustXyz
    object PackXyz00
}
RbacPermissions -[hidden]> BusinessObjects

UserMike o---> RoleAdministrators
UserSuse o--> RoleCustXyz_Admin
UserPaul o--> RolePackXyz00_Owner

RoleAdministrators o..> RoleCustXyz_Owner
RoleCustXyz_Owner o-> RoleCustXyz_Admin
RoleCustXyz_Admin o-> RolePackXyz00_Owner

RoleCustXyz_Owner o--> PermCustXyz_Edit
RoleCustXyz_Owner o--> PermCustXyz_Delete
RoleCustXyz_Admin o--> PermCustXyz_View
RoleCustXyz_Admin o--> PermCustXyz_AddPackage
RolePackXyz00_Owner o--> PermPackXyz00_View
RolePackXyz00_Owner o--> PermPackXyz00_Edit
RolePackXyz00_Owner o--> PermPackXyz00_Delete
RolePackXyz00_Owner o--> PermPackXyz00_AddUser

PermCustXyz_View o--> CustXyz
PermCustXyz_Edit o--> CustXyz
PermCustXyz_Delete o--> CustXyz
PermCustXyz_AddPackage o--> CustXyz
PermPackXyz00_View o--> PackXyz00
PermPackXyz00_Edit o--> PackXyz00
PermPackXyz00_Delete o--> PackXyz00
PermPackXyz00_AddUser o--> PackXyz00

@enduml
```


```plantuml
@startuml
left to right direction
' top to bottom direction

' hide the ugly E in a circle left to the entity name
hide circle

' use right-angled line routing
' skinparam linetype ortho

package rbacPerms {
    cust
}

package rbacRoles {
    entity administrators
    entity custXXX
}

package rbacUsers {
    entity adminMike
    adminMike <-- administrators

    entity adminSven
    entity custXXX
    entity pacAdmXXX00
}

@enduml
```

