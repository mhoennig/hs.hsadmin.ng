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

        role:debitorRel.anchorPerson:owner[[debitorRel.anchorPerson:owner]]
        role:debitorRel.anchorPerson:admin[[debitorRel.anchorPerson:admin]]
        role:debitorRel.anchorPerson:referrer[[debitorRel.anchorPerson:referrer]]
    end
end

subgraph debitorRel.holderPerson["`**debitorRel.holderPerson**`"]
    direction TB
    style debitorRel.holderPerson fill:#99bcdb,stroke:#274d6e,stroke-width:8px

    subgraph debitorRel.holderPerson:roles[ ]
        style debitorRel.holderPerson:roles fill:#99bcdb,stroke:white

        role:debitorRel.holderPerson:owner[[debitorRel.holderPerson:owner]]
        role:debitorRel.holderPerson:admin[[debitorRel.holderPerson:admin]]
        role:debitorRel.holderPerson:referrer[[debitorRel.holderPerson:referrer]]
    end
end

subgraph partnerRel.holderPerson["`**partnerRel.holderPerson**`"]
    direction TB
    style partnerRel.holderPerson fill:#99bcdb,stroke:#274d6e,stroke-width:8px

    subgraph partnerRel.holderPerson:roles[ ]
        style partnerRel.holderPerson:roles fill:#99bcdb,stroke:white

        role:partnerRel.holderPerson:owner[[partnerRel.holderPerson:owner]]
        role:partnerRel.holderPerson:admin[[partnerRel.holderPerson:admin]]
        role:partnerRel.holderPerson:referrer[[partnerRel.holderPerson:referrer]]
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
        subgraph debitorRel.anchorPerson["`**debitorRel.anchorPerson**`"]
            direction TB
            style debitorRel.anchorPerson fill:#99bcdb,stroke:#274d6e,stroke-width:8px

            subgraph debitorRel.anchorPerson:roles[ ]
                style debitorRel.anchorPerson:roles fill:#99bcdb,stroke:white

                role:debitorRel.anchorPerson:owner[[debitorRel.anchorPerson:owner]]
                role:debitorRel.anchorPerson:admin[[debitorRel.anchorPerson:admin]]
                role:debitorRel.anchorPerson:referrer[[debitorRel.anchorPerson:referrer]]
            end
        end

        subgraph debitorRel.holderPerson["`**debitorRel.holderPerson**`"]
            direction TB
            style debitorRel.holderPerson fill:#99bcdb,stroke:#274d6e,stroke-width:8px

            subgraph debitorRel.holderPerson:roles[ ]
                style debitorRel.holderPerson:roles fill:#99bcdb,stroke:white

                role:debitorRel.holderPerson:owner[[debitorRel.holderPerson:owner]]
                role:debitorRel.holderPerson:admin[[debitorRel.holderPerson:admin]]
                role:debitorRel.holderPerson:referrer[[debitorRel.holderPerson:referrer]]
            end
        end

        subgraph debitorRel.contact["`**debitorRel.contact**`"]
            direction TB
            style debitorRel.contact fill:#99bcdb,stroke:#274d6e,stroke-width:8px

            subgraph debitorRel.contact:roles[ ]
                style debitorRel.contact:roles fill:#99bcdb,stroke:white

                role:debitorRel.contact:owner[[debitorRel.contact:owner]]
                role:debitorRel.contact:admin[[debitorRel.contact:admin]]
                role:debitorRel.contact:referrer[[debitorRel.contact:referrer]]
            end
        end

        subgraph debitorRel:roles[ ]
            style debitorRel:roles fill:#99bcdb,stroke:white

            role:debitorRel:owner[[debitorRel:owner]]
            role:debitorRel:admin[[debitorRel:admin]]
            role:debitorRel:agent[[debitorRel:agent]]
            role:debitorRel:tenant[[debitorRel:tenant]]
        end
    end
end

subgraph partnerRel["`**partnerRel**`"]
    direction TB
    style partnerRel fill:#99bcdb,stroke:#274d6e,stroke-width:8px

    subgraph partnerRel.holderPerson["`**partnerRel.holderPerson**`"]
        direction TB
        style partnerRel.holderPerson fill:#99bcdb,stroke:#274d6e,stroke-width:8px

        subgraph partnerRel.holderPerson:roles[ ]
            style partnerRel.holderPerson:roles fill:#99bcdb,stroke:white

            role:partnerRel.holderPerson:owner[[partnerRel.holderPerson:owner]]
            role:partnerRel.holderPerson:admin[[partnerRel.holderPerson:admin]]
            role:partnerRel.holderPerson:referrer[[partnerRel.holderPerson:referrer]]
        end
    end

    subgraph partnerRel.contact["`**partnerRel.contact**`"]
        direction TB
        style partnerRel.contact fill:#99bcdb,stroke:#274d6e,stroke-width:8px

        subgraph partnerRel.contact:roles[ ]
            style partnerRel.contact:roles fill:#99bcdb,stroke:white

            role:partnerRel.contact:owner[[partnerRel.contact:owner]]
            role:partnerRel.contact:admin[[partnerRel.contact:admin]]
            role:partnerRel.contact:referrer[[partnerRel.contact:referrer]]
        end
    end

    subgraph partnerRel.anchorPerson["`**partnerRel.anchorPerson**`"]
        direction TB
        style partnerRel.anchorPerson fill:#99bcdb,stroke:#274d6e,stroke-width:8px

        subgraph partnerRel.anchorPerson:roles[ ]
            style partnerRel.anchorPerson:roles fill:#99bcdb,stroke:white

            role:partnerRel.anchorPerson:owner[[partnerRel.anchorPerson:owner]]
            role:partnerRel.anchorPerson:admin[[partnerRel.anchorPerson:admin]]
            role:partnerRel.anchorPerson:referrer[[partnerRel.anchorPerson:referrer]]
        end
    end

    subgraph partnerRel:roles[ ]
        style partnerRel:roles fill:#99bcdb,stroke:white

        role:partnerRel:owner[[partnerRel:owner]]
        role:partnerRel:admin[[partnerRel:admin]]
        role:partnerRel:agent[[partnerRel:agent]]
        role:partnerRel:tenant[[partnerRel:tenant]]
    end
end

subgraph partnerRel.contact["`**partnerRel.contact**`"]
    direction TB
    style partnerRel.contact fill:#99bcdb,stroke:#274d6e,stroke-width:8px

    subgraph partnerRel.contact:roles[ ]
        style partnerRel.contact:roles fill:#99bcdb,stroke:white

        role:partnerRel.contact:owner[[partnerRel.contact:owner]]
        role:partnerRel.contact:admin[[partnerRel.contact:admin]]
        role:partnerRel.contact:referrer[[partnerRel.contact:referrer]]
    end
end

subgraph debitorRel.contact["`**debitorRel.contact**`"]
    direction TB
    style debitorRel.contact fill:#99bcdb,stroke:#274d6e,stroke-width:8px

    subgraph debitorRel.contact:roles[ ]
        style debitorRel.contact:roles fill:#99bcdb,stroke:white

        role:debitorRel.contact:owner[[debitorRel.contact:owner]]
        role:debitorRel.contact:admin[[debitorRel.contact:admin]]
        role:debitorRel.contact:referrer[[debitorRel.contact:referrer]]
    end
end

subgraph partnerRel.anchorPerson["`**partnerRel.anchorPerson**`"]
    direction TB
    style partnerRel.anchorPerson fill:#99bcdb,stroke:#274d6e,stroke-width:8px

    subgraph partnerRel.anchorPerson:roles[ ]
        style partnerRel.anchorPerson:roles fill:#99bcdb,stroke:white

        role:partnerRel.anchorPerson:owner[[partnerRel.anchorPerson:owner]]
        role:partnerRel.anchorPerson:admin[[partnerRel.anchorPerson:admin]]
        role:partnerRel.anchorPerson:referrer[[partnerRel.anchorPerson:referrer]]
    end
end

subgraph refundBankAccount["`**refundBankAccount**`"]
    direction TB
    style refundBankAccount fill:#99bcdb,stroke:#274d6e,stroke-width:8px

    subgraph refundBankAccount:roles[ ]
        style refundBankAccount:roles fill:#99bcdb,stroke:white

        role:refundBankAccount:owner[[refundBankAccount:owner]]
        role:refundBankAccount:admin[[refundBankAccount:admin]]
        role:refundBankAccount:referrer[[refundBankAccount:referrer]]
    end
end

%% granting roles to roles
role:global:admin -.-> role:debitorRel.anchorPerson:owner
role:debitorRel.anchorPerson:owner -.-> role:debitorRel.anchorPerson:admin
role:debitorRel.anchorPerson:admin -.-> role:debitorRel.anchorPerson:referrer
role:global:admin -.-> role:debitorRel.holderPerson:owner
role:debitorRel.holderPerson:owner -.-> role:debitorRel.holderPerson:admin
role:debitorRel.holderPerson:admin -.-> role:debitorRel.holderPerson:referrer
role:global:admin -.-> role:debitorRel.contact:owner
role:debitorRel.contact:owner -.-> role:debitorRel.contact:admin
role:debitorRel.contact:admin -.-> role:debitorRel.contact:referrer
role:global:admin -.-> role:debitorRel:owner
role:debitorRel:owner -.-> role:debitorRel:admin
role:debitorRel.anchorPerson:admin -.-> role:debitorRel:admin
role:debitorRel:admin -.-> role:debitorRel:agent
role:debitorRel.holderPerson:admin -.-> role:debitorRel:agent
role:debitorRel:agent -.-> role:debitorRel:tenant
role:debitorRel.holderPerson:admin -.-> role:debitorRel:tenant
role:debitorRel.contact:admin -.-> role:debitorRel:tenant
role:debitorRel:tenant -.-> role:debitorRel.anchorPerson:referrer
role:debitorRel:tenant -.-> role:debitorRel.holderPerson:referrer
role:debitorRel:tenant -.-> role:debitorRel.contact:referrer
role:global:admin -.-> role:refundBankAccount:owner
role:refundBankAccount:owner -.-> role:refundBankAccount:admin
role:refundBankAccount:admin -.-> role:refundBankAccount:referrer
role:refundBankAccount:admin ==> role:debitorRel:agent
role:debitorRel:agent ==> role:refundBankAccount:referrer
role:global:admin -.-> role:partnerRel.anchorPerson:owner
role:partnerRel.anchorPerson:owner -.-> role:partnerRel.anchorPerson:admin
role:partnerRel.anchorPerson:admin -.-> role:partnerRel.anchorPerson:referrer
role:global:admin -.-> role:partnerRel.holderPerson:owner
role:partnerRel.holderPerson:owner -.-> role:partnerRel.holderPerson:admin
role:partnerRel.holderPerson:admin -.-> role:partnerRel.holderPerson:referrer
role:global:admin -.-> role:partnerRel.contact:owner
role:partnerRel.contact:owner -.-> role:partnerRel.contact:admin
role:partnerRel.contact:admin -.-> role:partnerRel.contact:referrer
role:global:admin -.-> role:partnerRel:owner
role:partnerRel:owner -.-> role:partnerRel:admin
role:partnerRel.anchorPerson:admin -.-> role:partnerRel:admin
role:partnerRel:admin -.-> role:partnerRel:agent
role:partnerRel.holderPerson:admin -.-> role:partnerRel:agent
role:partnerRel:agent -.-> role:partnerRel:tenant
role:partnerRel.holderPerson:admin -.-> role:partnerRel:tenant
role:partnerRel.contact:admin -.-> role:partnerRel:tenant
role:partnerRel:tenant -.-> role:partnerRel.anchorPerson:referrer
role:partnerRel:tenant -.-> role:partnerRel.holderPerson:referrer
role:partnerRel:tenant -.-> role:partnerRel.contact:referrer
role:partnerRel:admin ==> role:debitorRel:admin
role:partnerRel:agent ==> role:debitorRel:agent
role:debitorRel:agent ==> role:partnerRel:tenant

%% granting permissions to roles
role:global:admin ==> perm:debitor:INSERT
role:debitorRel:owner ==> perm:debitor:DELETE
role:debitorRel:admin ==> perm:debitor:UPDATE
role:debitorRel:tenant ==> perm:debitor:SELECT

```
