### rbac bookingItem

This code generated was by RbacViewMermaidFlowchartGenerator, do not amend manually.

```mermaid
%%{init:{'flowchart':{'htmlLabels':false}}}%%
flowchart TB

subgraph debitor.debitorRel.anchorPerson["`**debitor.debitorRel.anchorPerson**`"]
    direction TB
    style debitor.debitorRel.anchorPerson fill:#99bcdb,stroke:#274d6e,stroke-width:8px

    subgraph debitor.debitorRel.anchorPerson:roles[ ]
        style debitor.debitorRel.anchorPerson:roles fill:#99bcdb,stroke:white

        role:debitor.debitorRel.anchorPerson:OWNER[[debitor.debitorRel.anchorPerson:OWNER]]
        role:debitor.debitorRel.anchorPerson:ADMIN[[debitor.debitorRel.anchorPerson:ADMIN]]
        role:debitor.debitorRel.anchorPerson:REFERRER[[debitor.debitorRel.anchorPerson:REFERRER]]
    end
end

subgraph debitor.debitorRel.holderPerson["`**debitor.debitorRel.holderPerson**`"]
    direction TB
    style debitor.debitorRel.holderPerson fill:#99bcdb,stroke:#274d6e,stroke-width:8px

    subgraph debitor.debitorRel.holderPerson:roles[ ]
        style debitor.debitorRel.holderPerson:roles fill:#99bcdb,stroke:white

        role:debitor.debitorRel.holderPerson:OWNER[[debitor.debitorRel.holderPerson:OWNER]]
        role:debitor.debitorRel.holderPerson:ADMIN[[debitor.debitorRel.holderPerson:ADMIN]]
        role:debitor.debitorRel.holderPerson:REFERRER[[debitor.debitorRel.holderPerson:REFERRER]]
    end
end

subgraph debitorRel.anchorPerson["`**debitorRel.anchorPerson**`"]
    direction TB
    style debitorRel.anchorPerson fill:#99bcdb,stroke:#274d6e,stroke-width:8px

    subgraph debitorRel.anchorPerson:roles[ ]
        style debitorRel.anchorPerson:roles fill:#99bcdb,stroke:white

        role:debitorRel.anchorPerson:OWNER[[debitorRel.anchorPerson:OWNER]]
        role:debitorRel.anchorPerson:ADMIN[[debitorRel.anchorPerson:ADMIN]]
        role:debitorRel.anchorPerson:REFERRER[[debitorRel.anchorPerson:REFERRER]]
    end
end

subgraph debitorRel.holderPerson["`**debitorRel.holderPerson**`"]
    direction TB
    style debitorRel.holderPerson fill:#99bcdb,stroke:#274d6e,stroke-width:8px

    subgraph debitorRel.holderPerson:roles[ ]
        style debitorRel.holderPerson:roles fill:#99bcdb,stroke:white

        role:debitorRel.holderPerson:OWNER[[debitorRel.holderPerson:OWNER]]
        role:debitorRel.holderPerson:ADMIN[[debitorRel.holderPerson:ADMIN]]
        role:debitorRel.holderPerson:REFERRER[[debitorRel.holderPerson:REFERRER]]
    end
end

subgraph debitor.debitorRel["`**debitor.debitorRel**`"]
    direction TB
    style debitor.debitorRel fill:#99bcdb,stroke:#274d6e,stroke-width:8px

    subgraph debitor.debitorRel:roles[ ]
        style debitor.debitorRel:roles fill:#99bcdb,stroke:white

        role:debitor.debitorRel:OWNER[[debitor.debitorRel:OWNER]]
        role:debitor.debitorRel:ADMIN[[debitor.debitorRel:ADMIN]]
        role:debitor.debitorRel:AGENT[[debitor.debitorRel:AGENT]]
        role:debitor.debitorRel:TENANT[[debitor.debitorRel:TENANT]]
    end
end

subgraph debitor.partnerRel["`**debitor.partnerRel**`"]
    direction TB
    style debitor.partnerRel fill:#99bcdb,stroke:#274d6e,stroke-width:8px

    subgraph debitor.partnerRel:roles[ ]
        style debitor.partnerRel:roles fill:#99bcdb,stroke:white

        role:debitor.partnerRel:OWNER[[debitor.partnerRel:OWNER]]
        role:debitor.partnerRel:ADMIN[[debitor.partnerRel:ADMIN]]
        role:debitor.partnerRel:AGENT[[debitor.partnerRel:AGENT]]
        role:debitor.partnerRel:TENANT[[debitor.partnerRel:TENANT]]
    end
end

subgraph bookingItem["`**bookingItem**`"]
    direction TB
    style bookingItem fill:#dd4901,stroke:#274d6e,stroke-width:8px

    subgraph bookingItem:roles[ ]
        style bookingItem:roles fill:#dd4901,stroke:white

        role:bookingItem:OWNER[[bookingItem:OWNER]]
        role:bookingItem:ADMIN[[bookingItem:ADMIN]]
        role:bookingItem:TENANT[[bookingItem:TENANT]]
    end

    subgraph bookingItem:permissions[ ]
        style bookingItem:permissions fill:#dd4901,stroke:white

        perm:bookingItem:INSERT{{bookingItem:INSERT}}
        perm:bookingItem:DELETE{{bookingItem:DELETE}}
        perm:bookingItem:UPDATE{{bookingItem:UPDATE}}
        perm:bookingItem:SELECT{{bookingItem:SELECT}}
    end
end

subgraph debitor.partnerRel.contact["`**debitor.partnerRel.contact**`"]
    direction TB
    style debitor.partnerRel.contact fill:#99bcdb,stroke:#274d6e,stroke-width:8px

    subgraph debitor.partnerRel.contact:roles[ ]
        style debitor.partnerRel.contact:roles fill:#99bcdb,stroke:white

        role:debitor.partnerRel.contact:OWNER[[debitor.partnerRel.contact:OWNER]]
        role:debitor.partnerRel.contact:ADMIN[[debitor.partnerRel.contact:ADMIN]]
        role:debitor.partnerRel.contact:REFERRER[[debitor.partnerRel.contact:REFERRER]]
    end
end

subgraph debitor.partnerRel.holderPerson["`**debitor.partnerRel.holderPerson**`"]
    direction TB
    style debitor.partnerRel.holderPerson fill:#99bcdb,stroke:#274d6e,stroke-width:8px

    subgraph debitor.partnerRel.holderPerson:roles[ ]
        style debitor.partnerRel.holderPerson:roles fill:#99bcdb,stroke:white

        role:debitor.partnerRel.holderPerson:OWNER[[debitor.partnerRel.holderPerson:OWNER]]
        role:debitor.partnerRel.holderPerson:ADMIN[[debitor.partnerRel.holderPerson:ADMIN]]
        role:debitor.partnerRel.holderPerson:REFERRER[[debitor.partnerRel.holderPerson:REFERRER]]
    end
end

subgraph debitor["`**debitor**`"]
    direction TB
    style debitor fill:#99bcdb,stroke:#274d6e,stroke-width:8px
end

subgraph debitor.refundBankAccount["`**debitor.refundBankAccount**`"]
    direction TB
    style debitor.refundBankAccount fill:#99bcdb,stroke:#274d6e,stroke-width:8px

    subgraph debitor.refundBankAccount:roles[ ]
        style debitor.refundBankAccount:roles fill:#99bcdb,stroke:white

        role:debitor.refundBankAccount:OWNER[[debitor.refundBankAccount:OWNER]]
        role:debitor.refundBankAccount:ADMIN[[debitor.refundBankAccount:ADMIN]]
        role:debitor.refundBankAccount:REFERRER[[debitor.refundBankAccount:REFERRER]]
    end
end

subgraph debitor.partnerRel.anchorPerson["`**debitor.partnerRel.anchorPerson**`"]
    direction TB
    style debitor.partnerRel.anchorPerson fill:#99bcdb,stroke:#274d6e,stroke-width:8px

    subgraph debitor.partnerRel.anchorPerson:roles[ ]
        style debitor.partnerRel.anchorPerson:roles fill:#99bcdb,stroke:white

        role:debitor.partnerRel.anchorPerson:OWNER[[debitor.partnerRel.anchorPerson:OWNER]]
        role:debitor.partnerRel.anchorPerson:ADMIN[[debitor.partnerRel.anchorPerson:ADMIN]]
        role:debitor.partnerRel.anchorPerson:REFERRER[[debitor.partnerRel.anchorPerson:REFERRER]]
    end
end

subgraph debitorRel.contact["`**debitorRel.contact**`"]
    direction TB
    style debitorRel.contact fill:#99bcdb,stroke:#274d6e,stroke-width:8px

    subgraph debitorRel.contact:roles[ ]
        style debitorRel.contact:roles fill:#99bcdb,stroke:white

        role:debitorRel.contact:OWNER[[debitorRel.contact:OWNER]]
        role:debitorRel.contact:ADMIN[[debitorRel.contact:ADMIN]]
        role:debitorRel.contact:REFERRER[[debitorRel.contact:REFERRER]]
    end
end

subgraph debitor.debitorRel.contact["`**debitor.debitorRel.contact**`"]
    direction TB
    style debitor.debitorRel.contact fill:#99bcdb,stroke:#274d6e,stroke-width:8px

    subgraph debitor.debitorRel.contact:roles[ ]
        style debitor.debitorRel.contact:roles fill:#99bcdb,stroke:white

        role:debitor.debitorRel.contact:OWNER[[debitor.debitorRel.contact:OWNER]]
        role:debitor.debitorRel.contact:ADMIN[[debitor.debitorRel.contact:ADMIN]]
        role:debitor.debitorRel.contact:REFERRER[[debitor.debitorRel.contact:REFERRER]]
    end
end

subgraph debitorRel["`**debitorRel**`"]
    direction TB
    style debitorRel fill:#99bcdb,stroke:#274d6e,stroke-width:8px

    subgraph debitorRel:roles[ ]
        style debitorRel:roles fill:#99bcdb,stroke:white

        role:debitorRel:OWNER[[debitorRel:OWNER]]
        role:debitorRel:ADMIN[[debitorRel:ADMIN]]
        role:debitorRel:AGENT[[debitorRel:AGENT]]
        role:debitorRel:TENANT[[debitorRel:TENANT]]
    end
end

%% granting roles to roles
role:global:ADMIN -.-> role:debitor.debitorRel.anchorPerson:OWNER
role:debitor.debitorRel.anchorPerson:OWNER -.-> role:debitor.debitorRel.anchorPerson:ADMIN
role:debitor.debitorRel.anchorPerson:ADMIN -.-> role:debitor.debitorRel.anchorPerson:REFERRER
role:global:ADMIN -.-> role:debitor.debitorRel.holderPerson:OWNER
role:debitor.debitorRel.holderPerson:OWNER -.-> role:debitor.debitorRel.holderPerson:ADMIN
role:debitor.debitorRel.holderPerson:ADMIN -.-> role:debitor.debitorRel.holderPerson:REFERRER
role:global:ADMIN -.-> role:debitor.debitorRel.contact:OWNER
role:debitor.debitorRel.contact:OWNER -.-> role:debitor.debitorRel.contact:ADMIN
role:debitor.debitorRel.contact:ADMIN -.-> role:debitor.debitorRel.contact:REFERRER
role:global:ADMIN -.-> role:debitor.debitorRel:OWNER
role:debitor.debitorRel:OWNER -.-> role:debitor.debitorRel:ADMIN
role:debitor.debitorRel:ADMIN -.-> role:debitor.debitorRel:AGENT
role:debitor.debitorRel:AGENT -.-> role:debitor.debitorRel:TENANT
role:debitor.debitorRel.contact:ADMIN -.-> role:debitor.debitorRel:TENANT
role:debitor.debitorRel:TENANT -.-> role:debitor.debitorRel.anchorPerson:REFERRER
role:debitor.debitorRel:TENANT -.-> role:debitor.debitorRel.holderPerson:REFERRER
role:debitor.debitorRel:TENANT -.-> role:debitor.debitorRel.contact:REFERRER
role:debitor.debitorRel.anchorPerson:ADMIN -.-> role:debitor.debitorRel:OWNER
role:debitor.debitorRel.holderPerson:ADMIN -.-> role:debitor.debitorRel:AGENT
role:global:ADMIN -.-> role:debitor.refundBankAccount:OWNER
role:debitor.refundBankAccount:OWNER -.-> role:debitor.refundBankAccount:ADMIN
role:debitor.refundBankAccount:ADMIN -.-> role:debitor.refundBankAccount:REFERRER
role:debitor.refundBankAccount:ADMIN -.-> role:debitor.debitorRel:AGENT
role:debitor.debitorRel:AGENT -.-> role:debitor.refundBankAccount:REFERRER
role:global:ADMIN -.-> role:debitor.partnerRel.anchorPerson:OWNER
role:debitor.partnerRel.anchorPerson:OWNER -.-> role:debitor.partnerRel.anchorPerson:ADMIN
role:debitor.partnerRel.anchorPerson:ADMIN -.-> role:debitor.partnerRel.anchorPerson:REFERRER
role:global:ADMIN -.-> role:debitor.partnerRel.holderPerson:OWNER
role:debitor.partnerRel.holderPerson:OWNER -.-> role:debitor.partnerRel.holderPerson:ADMIN
role:debitor.partnerRel.holderPerson:ADMIN -.-> role:debitor.partnerRel.holderPerson:REFERRER
role:global:ADMIN -.-> role:debitor.partnerRel.contact:OWNER
role:debitor.partnerRel.contact:OWNER -.-> role:debitor.partnerRel.contact:ADMIN
role:debitor.partnerRel.contact:ADMIN -.-> role:debitor.partnerRel.contact:REFERRER
role:global:ADMIN -.-> role:debitor.partnerRel:OWNER
role:debitor.partnerRel:OWNER -.-> role:debitor.partnerRel:ADMIN
role:debitor.partnerRel:ADMIN -.-> role:debitor.partnerRel:AGENT
role:debitor.partnerRel:AGENT -.-> role:debitor.partnerRel:TENANT
role:debitor.partnerRel.contact:ADMIN -.-> role:debitor.partnerRel:TENANT
role:debitor.partnerRel:TENANT -.-> role:debitor.partnerRel.anchorPerson:REFERRER
role:debitor.partnerRel:TENANT -.-> role:debitor.partnerRel.holderPerson:REFERRER
role:debitor.partnerRel:TENANT -.-> role:debitor.partnerRel.contact:REFERRER
role:debitor.partnerRel.anchorPerson:ADMIN -.-> role:debitor.partnerRel:OWNER
role:debitor.partnerRel.holderPerson:ADMIN -.-> role:debitor.partnerRel:AGENT
role:debitor.partnerRel:ADMIN -.-> role:debitor.debitorRel:ADMIN
role:debitor.partnerRel:AGENT -.-> role:debitor.debitorRel:AGENT
role:debitor.debitorRel:AGENT -.-> role:debitor.partnerRel:TENANT
role:global:ADMIN -.-> role:debitorRel.anchorPerson:OWNER
role:debitorRel.anchorPerson:OWNER -.-> role:debitorRel.anchorPerson:ADMIN
role:debitorRel.anchorPerson:ADMIN -.-> role:debitorRel.anchorPerson:REFERRER
role:global:ADMIN -.-> role:debitorRel.holderPerson:OWNER
role:debitorRel.holderPerson:OWNER -.-> role:debitorRel.holderPerson:ADMIN
role:debitorRel.holderPerson:ADMIN -.-> role:debitorRel.holderPerson:REFERRER
role:global:ADMIN -.-> role:debitorRel.contact:OWNER
role:debitorRel.contact:OWNER -.-> role:debitorRel.contact:ADMIN
role:debitorRel.contact:ADMIN -.-> role:debitorRel.contact:REFERRER
role:global:ADMIN -.-> role:debitorRel:OWNER
role:debitorRel:OWNER -.-> role:debitorRel:ADMIN
role:debitorRel:ADMIN -.-> role:debitorRel:AGENT
role:debitorRel:AGENT -.-> role:debitorRel:TENANT
role:debitorRel.contact:ADMIN -.-> role:debitorRel:TENANT
role:debitorRel:TENANT -.-> role:debitorRel.anchorPerson:REFERRER
role:debitorRel:TENANT -.-> role:debitorRel.holderPerson:REFERRER
role:debitorRel:TENANT -.-> role:debitorRel.contact:REFERRER
role:debitorRel.anchorPerson:ADMIN -.-> role:debitorRel:OWNER
role:debitorRel.holderPerson:ADMIN -.-> role:debitorRel:AGENT
role:debitorRel:AGENT ==> role:bookingItem:OWNER
role:bookingItem:OWNER ==> role:bookingItem:ADMIN
role:bookingItem:ADMIN ==> role:bookingItem:TENANT
role:bookingItem:TENANT ==> role:debitorRel:TENANT

%% granting permissions to roles
role:debitorRel:ADMIN ==> perm:bookingItem:INSERT
role:global:ADMIN ==> perm:bookingItem:DELETE
role:bookingItem:OWNER ==> perm:bookingItem:UPDATE
role:bookingItem:TENANT ==> perm:bookingItem:SELECT

```
