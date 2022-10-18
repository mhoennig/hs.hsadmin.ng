### hs_office_coopSharesTransaction RBAC

```mermaid
flowchart TB

subgraph hsOfficeMembership
    direction TB
    style hsOfficeMembership fill:#eee
    
    role:hsOfficeMembership.owner[membership.admin]    
    --> role:hsOfficeMembership.admin[membership.admin]    
    --> role:hsOfficeMembership.agent[membership.agent]    
    --> role:hsOfficeMembership.tenant[membership.tenant]    
    --> role:hsOfficeMembership.guest[membership.guest]   
    
    role:hsOfficePartner.agent --> role:hsOfficeMembership.agent
end

subgraph hsOfficeCoopSharesTransaction
                    
       role:hsOfficeMembership.admin
       --> perm:hsOfficeCoopSharesTransaction.create{{coopSharesTx.create}}
                    
       role:hsOfficeMembership.agent
        --> perm:hsOfficeCoopSharesTransaction.view{{coopSharesTx.view}}
end


```
