### rbac debitor

This code generated was by RbacViewMermaidFlowchartGenerator, do not amend manually.

```mermaid
%%{init:{'flowchart':{'htmlLabels':false}}}%%
flowchart TB

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

subgraph partnerRel.holderPerson["`**partnerRel.holderPerson**`"]
    direction TB
    style partnerRel.holderPerson fill:#99bcdb,stroke:#274d6e,stroke-width:8px

    subgraph partnerRel.holderPerson:roles[ ]
        style partnerRel.holderPerson:roles fill:#99bcdb,stroke:white

        role:partnerRel.holderPerson:OWNER[[partnerRel.holderPerson:OWNER]]
        role:partnerRel.holderPerson:ADMIN[[partnerRel.holderPerson:ADMIN]]
        role:partnerRel.holderPerson:REFERRER[[partnerRel.holderPerson:REFERRER]]
    end
end

subgraph debitor["`**debitor**`"]
    direction TB
    style debitor fill:#dd4901,stroke:#274d6e,stroke-width:8px

    subgraph debitor:permissions[ ]
        style debitor:permissions fill:#dd4901,stroke:white

        perm:debitor:INSERT{{debitor:INSERT}}
        perm:debitor:DELETE{{debitor:DELETE}}
        perm:debitor:UPDATE{{debitor:UPDATE}}
        perm:debitor:SELECT{{debitor:SELECT}}
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
end

subgraph partnerRel["`**partnerRel**`"]
    direction TB
    style partnerRel fill:#99bcdb,stroke:#274d6e,stroke-width:8px

    subgraph partnerRel:roles[ ]
        style partnerRel:roles fill:#99bcdb,stroke:white

        role:partnerRel:OWNER[[partnerRel:OWNER]]
        role:partnerRel:ADMIN[[partnerRel:ADMIN]]
        role:partnerRel:AGENT[[partnerRel:AGENT]]
        role:partnerRel:TENANT[[partnerRel:TENANT]]
    end
end

subgraph partnerRel.contact["`**partnerRel.contact**`"]
    direction TB
    style partnerRel.contact fill:#99bcdb,stroke:#274d6e,stroke-width:8px

    subgraph partnerRel.contact:roles[ ]
        style partnerRel.contact:roles fill:#99bcdb,stroke:white

        role:partnerRel.contact:OWNER[[partnerRel.contact:OWNER]]
        role:partnerRel.contact:ADMIN[[partnerRel.contact:ADMIN]]
        role:partnerRel.contact:REFERRER[[partnerRel.contact:REFERRER]]
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

subgraph partnerRel.anchorPerson["`**partnerRel.anchorPerson**`"]
    direction TB
    style partnerRel.anchorPerson fill:#99bcdb,stroke:#274d6e,stroke-width:8px

    subgraph partnerRel.anchorPerson:roles[ ]
        style partnerRel.anchorPerson:roles fill:#99bcdb,stroke:white

        role:partnerRel.anchorPerson:OWNER[[partnerRel.anchorPerson:OWNER]]
        role:partnerRel.anchorPerson:ADMIN[[partnerRel.anchorPerson:ADMIN]]
        role:partnerRel.anchorPerson:REFERRER[[partnerRel.anchorPerson:REFERRER]]
    end
end

subgraph refundBankAccount["`**refundBankAccount**`"]
    direction TB
    style refundBankAccount fill:#99bcdb,stroke:#274d6e,stroke-width:8px

    subgraph refundBankAccount:roles[ ]
        style refundBankAccount:roles fill:#99bcdb,stroke:white

        role:refundBankAccount:OWNER[[refundBankAccount:OWNER]]
        role:refundBankAccount:ADMIN[[refundBankAccount:ADMIN]]
        role:refundBankAccount:REFERRER[[refundBankAccount:REFERRER]]
    end
end

%% granting roles to roles
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
role:debitorRel.anchorPerson:ADMIN -.-> role:debitorRel:ADMIN
role:debitorRel:ADMIN -.-> role:debitorRel:AGENT
role:debitorRel.holderPerson:ADMIN -.-> role:debitorRel:AGENT
role:debitorRel:AGENT -.-> role:debitorRel:TENANT
role:debitorRel.holderPerson:ADMIN -.-> role:debitorRel:TENANT
role:debitorRel.contact:ADMIN -.-> role:debitorRel:TENANT
role:debitorRel:TENANT -.-> role:debitorRel.anchorPerson:REFERRER
role:debitorRel:TENANT -.-> role:debitorRel.holderPerson:REFERRER
role:debitorRel:TENANT -.-> role:debitorRel.contact:REFERRER
role:global:ADMIN -.-> role:refundBankAccount:OWNER
role:refundBankAccount:OWNER -.-> role:refundBankAccount:ADMIN
role:refundBankAccount:ADMIN -.-> role:refundBankAccount:REFERRER
role:refundBankAccount:ADMIN ==> role:debitorRel:AGENT
role:debitorRel:AGENT ==> role:refundBankAccount:REFERRER
role:global:ADMIN -.-> role:partnerRel.anchorPerson:OWNER
role:partnerRel.anchorPerson:OWNER -.-> role:partnerRel.anchorPerson:ADMIN
role:partnerRel.anchorPerson:ADMIN -.-> role:partnerRel.anchorPerson:REFERRER
role:global:ADMIN -.-> role:partnerRel.holderPerson:OWNER
role:partnerRel.holderPerson:OWNER -.-> role:partnerRel.holderPerson:ADMIN
role:partnerRel.holderPerson:ADMIN -.-> role:partnerRel.holderPerson:REFERRER
role:global:ADMIN -.-> role:partnerRel.contact:OWNER
role:partnerRel.contact:OWNER -.-> role:partnerRel.contact:ADMIN
role:partnerRel.contact:ADMIN -.-> role:partnerRel.contact:REFERRER
role:global:ADMIN -.-> role:partnerRel:OWNER
role:partnerRel:OWNER -.-> role:partnerRel:ADMIN
role:partnerRel.anchorPerson:ADMIN -.-> role:partnerRel:ADMIN
role:partnerRel:ADMIN -.-> role:partnerRel:AGENT
role:partnerRel.holderPerson:ADMIN -.-> role:partnerRel:AGENT
role:partnerRel:AGENT -.-> role:partnerRel:TENANT
role:partnerRel.holderPerson:ADMIN -.-> role:partnerRel:TENANT
role:partnerRel.contact:ADMIN -.-> role:partnerRel:TENANT
role:partnerRel:TENANT -.-> role:partnerRel.anchorPerson:REFERRER
role:partnerRel:TENANT -.-> role:partnerRel.holderPerson:REFERRER
role:partnerRel:TENANT -.-> role:partnerRel.contact:REFERRER
role:partnerRel:ADMIN ==> role:debitorRel:ADMIN
role:partnerRel:AGENT ==> role:debitorRel:AGENT
role:debitorRel:AGENT ==> role:partnerRel:TENANT

%% granting permissions to roles
role:global:ADMIN ==> perm:debitor:INSERT
role:debitorRel:OWNER ==> perm:debitor:DELETE
role:debitorRel:ADMIN ==> perm:debitor:UPDATE
role:debitorRel:TENANT ==> perm:debitor:SELECT

```
