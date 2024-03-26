### rbac partnerDetails

This code generated was by RbacViewMermaidFlowchartGenerator, do not amend manually.

```mermaid
%%{init:{'flowchart':{'htmlLabels':false}}}%%
flowchart TB

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

subgraph partnerDetails["`**partnerDetails**`"]
    direction TB
    style partnerDetails fill:#dd4901,stroke:#274d6e,stroke-width:8px

    subgraph partnerDetails:permissions[ ]
        style partnerDetails:permissions fill:#dd4901,stroke:white

        perm:partnerDetails:INSERT{{partnerDetails:INSERT}}
    end

    subgraph partnerRel["`**partnerRel**`"]
        direction TB
        style partnerRel fill:#99bcdb,stroke:#274d6e,stroke-width:8px
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

        subgraph partnerRel:roles[ ]
            style partnerRel:roles fill:#99bcdb,stroke:white

            role:partnerRel:owner[[partnerRel:owner]]
            role:partnerRel:admin[[partnerRel:admin]]
            role:partnerRel:agent[[partnerRel:agent]]
            role:partnerRel:tenant[[partnerRel:tenant]]
        end
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

%% granting roles to roles
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

%% granting permissions to roles
role:global:admin ==> perm:partnerDetails:INSERT

```
