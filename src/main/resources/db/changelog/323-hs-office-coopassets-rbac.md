### rbac coopAssetsTransaction

This code generated was by RbacViewMermaidFlowchartGenerator, do not amend manually.

```mermaid
%%{init:{'flowchart':{'htmlLabels':false}}}%%
flowchart TB

subgraph membership.partnerRel.holderPerson["`**membership.partnerRel.holderPerson**`"]
    direction TB
    style membership.partnerRel.holderPerson fill:#99bcdb,stroke:#274d6e,stroke-width:8px

    subgraph membership.partnerRel.holderPerson:roles[ ]
        style membership.partnerRel.holderPerson:roles fill:#99bcdb,stroke:white

        role:membership.partnerRel.holderPerson:owner[[membership.partnerRel.holderPerson:owner]]
        role:membership.partnerRel.holderPerson:admin[[membership.partnerRel.holderPerson:admin]]
        role:membership.partnerRel.holderPerson:referrer[[membership.partnerRel.holderPerson:referrer]]
    end
end

subgraph membership.partnerRel.anchorPerson["`**membership.partnerRel.anchorPerson**`"]
    direction TB
    style membership.partnerRel.anchorPerson fill:#99bcdb,stroke:#274d6e,stroke-width:8px

    subgraph membership.partnerRel.anchorPerson:roles[ ]
        style membership.partnerRel.anchorPerson:roles fill:#99bcdb,stroke:white

        role:membership.partnerRel.anchorPerson:owner[[membership.partnerRel.anchorPerson:owner]]
        role:membership.partnerRel.anchorPerson:admin[[membership.partnerRel.anchorPerson:admin]]
        role:membership.partnerRel.anchorPerson:referrer[[membership.partnerRel.anchorPerson:referrer]]
    end
end

subgraph coopAssetsTransaction["`**coopAssetsTransaction**`"]
    direction TB
    style coopAssetsTransaction fill:#dd4901,stroke:#274d6e,stroke-width:8px

    subgraph coopAssetsTransaction:permissions[ ]
        style coopAssetsTransaction:permissions fill:#dd4901,stroke:white

        perm:coopAssetsTransaction:INSERT{{coopAssetsTransaction:INSERT}}
        perm:coopAssetsTransaction:UPDATE{{coopAssetsTransaction:UPDATE}}
        perm:coopAssetsTransaction:SELECT{{coopAssetsTransaction:SELECT}}
    end
end

subgraph membership["`**membership**`"]
    direction TB
    style membership fill:#99bcdb,stroke:#274d6e,stroke-width:8px

    subgraph membership.partnerRel.holderPerson["`**membership.partnerRel.holderPerson**`"]
        direction TB
        style membership.partnerRel.holderPerson fill:#99bcdb,stroke:#274d6e,stroke-width:8px

        subgraph membership.partnerRel.holderPerson:roles[ ]
            style membership.partnerRel.holderPerson:roles fill:#99bcdb,stroke:white

            role:membership.partnerRel.holderPerson:owner[[membership.partnerRel.holderPerson:owner]]
            role:membership.partnerRel.holderPerson:admin[[membership.partnerRel.holderPerson:admin]]
            role:membership.partnerRel.holderPerson:referrer[[membership.partnerRel.holderPerson:referrer]]
        end
    end

    subgraph membership.partnerRel.anchorPerson["`**membership.partnerRel.anchorPerson**`"]
        direction TB
        style membership.partnerRel.anchorPerson fill:#99bcdb,stroke:#274d6e,stroke-width:8px

        subgraph membership.partnerRel.anchorPerson:roles[ ]
            style membership.partnerRel.anchorPerson:roles fill:#99bcdb,stroke:white

            role:membership.partnerRel.anchorPerson:owner[[membership.partnerRel.anchorPerson:owner]]
            role:membership.partnerRel.anchorPerson:admin[[membership.partnerRel.anchorPerson:admin]]
            role:membership.partnerRel.anchorPerson:referrer[[membership.partnerRel.anchorPerson:referrer]]
        end
    end

    subgraph membership.partnerRel["`**membership.partnerRel**`"]
        direction TB
        style membership.partnerRel fill:#99bcdb,stroke:#274d6e,stroke-width:8px
        subgraph membership.partnerRel.holderPerson["`**membership.partnerRel.holderPerson**`"]
            direction TB
            style membership.partnerRel.holderPerson fill:#99bcdb,stroke:#274d6e,stroke-width:8px

            subgraph membership.partnerRel.holderPerson:roles[ ]
                style membership.partnerRel.holderPerson:roles fill:#99bcdb,stroke:white

                role:membership.partnerRel.holderPerson:owner[[membership.partnerRel.holderPerson:owner]]
                role:membership.partnerRel.holderPerson:admin[[membership.partnerRel.holderPerson:admin]]
                role:membership.partnerRel.holderPerson:referrer[[membership.partnerRel.holderPerson:referrer]]
            end
        end

        subgraph membership.partnerRel.anchorPerson["`**membership.partnerRel.anchorPerson**`"]
            direction TB
            style membership.partnerRel.anchorPerson fill:#99bcdb,stroke:#274d6e,stroke-width:8px

            subgraph membership.partnerRel.anchorPerson:roles[ ]
                style membership.partnerRel.anchorPerson:roles fill:#99bcdb,stroke:white

                role:membership.partnerRel.anchorPerson:owner[[membership.partnerRel.anchorPerson:owner]]
                role:membership.partnerRel.anchorPerson:admin[[membership.partnerRel.anchorPerson:admin]]
                role:membership.partnerRel.anchorPerson:referrer[[membership.partnerRel.anchorPerson:referrer]]
            end
        end

        subgraph membership.partnerRel.contact["`**membership.partnerRel.contact**`"]
            direction TB
            style membership.partnerRel.contact fill:#99bcdb,stroke:#274d6e,stroke-width:8px

            subgraph membership.partnerRel.contact:roles[ ]
                style membership.partnerRel.contact:roles fill:#99bcdb,stroke:white

                role:membership.partnerRel.contact:owner[[membership.partnerRel.contact:owner]]
                role:membership.partnerRel.contact:admin[[membership.partnerRel.contact:admin]]
                role:membership.partnerRel.contact:referrer[[membership.partnerRel.contact:referrer]]
            end
        end

        subgraph membership.partnerRel:roles[ ]
            style membership.partnerRel:roles fill:#99bcdb,stroke:white

            role:membership.partnerRel:owner[[membership.partnerRel:owner]]
            role:membership.partnerRel:admin[[membership.partnerRel:admin]]
            role:membership.partnerRel:agent[[membership.partnerRel:agent]]
            role:membership.partnerRel:tenant[[membership.partnerRel:tenant]]
        end
    end

    subgraph membership.partnerRel.contact["`**membership.partnerRel.contact**`"]
        direction TB
        style membership.partnerRel.contact fill:#99bcdb,stroke:#274d6e,stroke-width:8px

        subgraph membership.partnerRel.contact:roles[ ]
            style membership.partnerRel.contact:roles fill:#99bcdb,stroke:white

            role:membership.partnerRel.contact:owner[[membership.partnerRel.contact:owner]]
            role:membership.partnerRel.contact:admin[[membership.partnerRel.contact:admin]]
            role:membership.partnerRel.contact:referrer[[membership.partnerRel.contact:referrer]]
        end
    end

    subgraph membership:roles[ ]
        style membership:roles fill:#99bcdb,stroke:white

        role:membership:owner[[membership:owner]]
        role:membership:admin[[membership:admin]]
        role:membership:agent[[membership:agent]]
    end
end

subgraph membership.partnerRel["`**membership.partnerRel**`"]
    direction TB
    style membership.partnerRel fill:#99bcdb,stroke:#274d6e,stroke-width:8px

    subgraph membership.partnerRel.holderPerson["`**membership.partnerRel.holderPerson**`"]
        direction TB
        style membership.partnerRel.holderPerson fill:#99bcdb,stroke:#274d6e,stroke-width:8px

        subgraph membership.partnerRel.holderPerson:roles[ ]
            style membership.partnerRel.holderPerson:roles fill:#99bcdb,stroke:white

            role:membership.partnerRel.holderPerson:owner[[membership.partnerRel.holderPerson:owner]]
            role:membership.partnerRel.holderPerson:admin[[membership.partnerRel.holderPerson:admin]]
            role:membership.partnerRel.holderPerson:referrer[[membership.partnerRel.holderPerson:referrer]]
        end
    end

    subgraph membership.partnerRel.anchorPerson["`**membership.partnerRel.anchorPerson**`"]
        direction TB
        style membership.partnerRel.anchorPerson fill:#99bcdb,stroke:#274d6e,stroke-width:8px

        subgraph membership.partnerRel.anchorPerson:roles[ ]
            style membership.partnerRel.anchorPerson:roles fill:#99bcdb,stroke:white

            role:membership.partnerRel.anchorPerson:owner[[membership.partnerRel.anchorPerson:owner]]
            role:membership.partnerRel.anchorPerson:admin[[membership.partnerRel.anchorPerson:admin]]
            role:membership.partnerRel.anchorPerson:referrer[[membership.partnerRel.anchorPerson:referrer]]
        end
    end

    subgraph membership.partnerRel.contact["`**membership.partnerRel.contact**`"]
        direction TB
        style membership.partnerRel.contact fill:#99bcdb,stroke:#274d6e,stroke-width:8px

        subgraph membership.partnerRel.contact:roles[ ]
            style membership.partnerRel.contact:roles fill:#99bcdb,stroke:white

            role:membership.partnerRel.contact:owner[[membership.partnerRel.contact:owner]]
            role:membership.partnerRel.contact:admin[[membership.partnerRel.contact:admin]]
            role:membership.partnerRel.contact:referrer[[membership.partnerRel.contact:referrer]]
        end
    end

    subgraph membership.partnerRel:roles[ ]
        style membership.partnerRel:roles fill:#99bcdb,stroke:white

        role:membership.partnerRel:owner[[membership.partnerRel:owner]]
        role:membership.partnerRel:admin[[membership.partnerRel:admin]]
        role:membership.partnerRel:agent[[membership.partnerRel:agent]]
        role:membership.partnerRel:tenant[[membership.partnerRel:tenant]]
    end
end

subgraph membership.partnerRel.contact["`**membership.partnerRel.contact**`"]
    direction TB
    style membership.partnerRel.contact fill:#99bcdb,stroke:#274d6e,stroke-width:8px

    subgraph membership.partnerRel.contact:roles[ ]
        style membership.partnerRel.contact:roles fill:#99bcdb,stroke:white

        role:membership.partnerRel.contact:owner[[membership.partnerRel.contact:owner]]
        role:membership.partnerRel.contact:admin[[membership.partnerRel.contact:admin]]
        role:membership.partnerRel.contact:referrer[[membership.partnerRel.contact:referrer]]
    end
end

%% granting roles to roles
role:global:admin -.-> role:membership.partnerRel.anchorPerson:owner
role:membership.partnerRel.anchorPerson:owner -.-> role:membership.partnerRel.anchorPerson:admin
role:membership.partnerRel.anchorPerson:admin -.-> role:membership.partnerRel.anchorPerson:referrer
role:global:admin -.-> role:membership.partnerRel.holderPerson:owner
role:membership.partnerRel.holderPerson:owner -.-> role:membership.partnerRel.holderPerson:admin
role:membership.partnerRel.holderPerson:admin -.-> role:membership.partnerRel.holderPerson:referrer
role:global:admin -.-> role:membership.partnerRel.contact:owner
role:membership.partnerRel.contact:owner -.-> role:membership.partnerRel.contact:admin
role:membership.partnerRel.contact:admin -.-> role:membership.partnerRel.contact:referrer
role:global:admin -.-> role:membership.partnerRel:owner
role:membership.partnerRel:owner -.-> role:membership.partnerRel:admin
role:membership.partnerRel.anchorPerson:admin -.-> role:membership.partnerRel:admin
role:membership.partnerRel:admin -.-> role:membership.partnerRel:agent
role:membership.partnerRel.holderPerson:admin -.-> role:membership.partnerRel:agent
role:membership.partnerRel:agent -.-> role:membership.partnerRel:tenant
role:membership.partnerRel.holderPerson:admin -.-> role:membership.partnerRel:tenant
role:membership.partnerRel.contact:admin -.-> role:membership.partnerRel:tenant
role:membership.partnerRel:tenant -.-> role:membership.partnerRel.anchorPerson:referrer
role:membership.partnerRel:tenant -.-> role:membership.partnerRel.holderPerson:referrer
role:membership.partnerRel:tenant -.-> role:membership.partnerRel.contact:referrer
role:membership:owner -.-> role:membership:admin
role:membership.partnerRel:admin -.-> role:membership:admin
role:membership:admin -.-> role:membership:agent
role:membership.partnerRel:agent -.-> role:membership:agent
role:membership:agent -.-> role:membership.partnerRel:tenant

%% granting permissions to roles
role:membership:admin ==> perm:coopAssetsTransaction:INSERT
role:membership:admin ==> perm:coopAssetsTransaction:UPDATE
role:membership:agent ==> perm:coopAssetsTransaction:SELECT

```
