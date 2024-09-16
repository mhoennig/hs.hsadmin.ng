### rbac coopAssetsTransaction

This code generated was by RbacViewMermaidFlowchartGenerator, do not amend manually.

```mermaid
%%{init:{'flowchart':{'htmlLabels':false}}}%%
flowchart TB

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

    subgraph membership:roles[ ]
        style membership:roles fill:#99bcdb,stroke:white

        role:membership:OWNER[[membership:OWNER]]
        role:membership:ADMIN[[membership:ADMIN]]
        role:membership:AGENT[[membership:AGENT]]
    end
end

subgraph membership.partnerRel["`**membership.partnerRel**`"]
    direction TB
    style membership.partnerRel fill:#99bcdb,stroke:#274d6e,stroke-width:8px

    subgraph membership.partnerRel:roles[ ]
        style membership.partnerRel:roles fill:#99bcdb,stroke:white

        role:membership.partnerRel:OWNER[[membership.partnerRel:OWNER]]
        role:membership.partnerRel:ADMIN[[membership.partnerRel:ADMIN]]
        role:membership.partnerRel:AGENT[[membership.partnerRel:AGENT]]
        role:membership.partnerRel:TENANT[[membership.partnerRel:TENANT]]
    end
end

subgraph membership.partnerRel.anchorPerson["`**membership.partnerRel.anchorPerson**`"]
    direction TB
    style membership.partnerRel.anchorPerson fill:#99bcdb,stroke:#274d6e,stroke-width:8px

    subgraph membership.partnerRel.anchorPerson:roles[ ]
        style membership.partnerRel.anchorPerson:roles fill:#99bcdb,stroke:white

        role:membership.partnerRel.anchorPerson:OWNER[[membership.partnerRel.anchorPerson:OWNER]]
        role:membership.partnerRel.anchorPerson:ADMIN[[membership.partnerRel.anchorPerson:ADMIN]]
        role:membership.partnerRel.anchorPerson:REFERRER[[membership.partnerRel.anchorPerson:REFERRER]]
    end
end

subgraph membership.partnerRel.contact["`**membership.partnerRel.contact**`"]
    direction TB
    style membership.partnerRel.contact fill:#99bcdb,stroke:#274d6e,stroke-width:8px

    subgraph membership.partnerRel.contact:roles[ ]
        style membership.partnerRel.contact:roles fill:#99bcdb,stroke:white

        role:membership.partnerRel.contact:OWNER[[membership.partnerRel.contact:OWNER]]
        role:membership.partnerRel.contact:ADMIN[[membership.partnerRel.contact:ADMIN]]
        role:membership.partnerRel.contact:REFERRER[[membership.partnerRel.contact:REFERRER]]
    end
end

subgraph membership.partnerRel.holderPerson["`**membership.partnerRel.holderPerson**`"]
    direction TB
    style membership.partnerRel.holderPerson fill:#99bcdb,stroke:#274d6e,stroke-width:8px

    subgraph membership.partnerRel.holderPerson:roles[ ]
        style membership.partnerRel.holderPerson:roles fill:#99bcdb,stroke:white

        role:membership.partnerRel.holderPerson:OWNER[[membership.partnerRel.holderPerson:OWNER]]
        role:membership.partnerRel.holderPerson:ADMIN[[membership.partnerRel.holderPerson:ADMIN]]
        role:membership.partnerRel.holderPerson:REFERRER[[membership.partnerRel.holderPerson:REFERRER]]
    end
end

%% granting roles to roles
role:rbac.global:ADMIN -.-> role:membership.partnerRel.anchorPerson:OWNER
role:membership.partnerRel.anchorPerson:OWNER -.-> role:membership.partnerRel.anchorPerson:ADMIN
role:membership.partnerRel.anchorPerson:ADMIN -.-> role:membership.partnerRel.anchorPerson:REFERRER
role:rbac.global:ADMIN -.-> role:membership.partnerRel.holderPerson:OWNER
role:membership.partnerRel.holderPerson:OWNER -.-> role:membership.partnerRel.holderPerson:ADMIN
role:membership.partnerRel.holderPerson:ADMIN -.-> role:membership.partnerRel.holderPerson:REFERRER
role:rbac.global:ADMIN -.-> role:membership.partnerRel.contact:OWNER
role:membership.partnerRel.contact:OWNER -.-> role:membership.partnerRel.contact:ADMIN
role:membership.partnerRel.contact:ADMIN -.-> role:membership.partnerRel.contact:REFERRER
role:rbac.global:ADMIN -.-> role:membership.partnerRel:OWNER
role:membership.partnerRel:OWNER -.-> role:membership.partnerRel:ADMIN
role:membership.partnerRel:ADMIN -.-> role:membership.partnerRel:AGENT
role:membership.partnerRel:AGENT -.-> role:membership.partnerRel:TENANT
role:membership.partnerRel.contact:ADMIN -.-> role:membership.partnerRel:TENANT
role:membership.partnerRel:TENANT -.-> role:membership.partnerRel.anchorPerson:REFERRER
role:membership.partnerRel:TENANT -.-> role:membership.partnerRel.holderPerson:REFERRER
role:membership.partnerRel:TENANT -.-> role:membership.partnerRel.contact:REFERRER
role:membership.partnerRel.anchorPerson:ADMIN -.-> role:membership.partnerRel:OWNER
role:membership.partnerRel.holderPerson:ADMIN -.-> role:membership.partnerRel:AGENT
role:membership:OWNER -.-> role:membership:ADMIN
role:membership.partnerRel:ADMIN -.-> role:membership:ADMIN
role:membership:ADMIN -.-> role:membership:AGENT
role:membership.partnerRel:AGENT -.-> role:membership:AGENT
role:membership:AGENT -.-> role:membership.partnerRel:TENANT

%% granting permissions to roles
role:membership:ADMIN ==> perm:coopAssetsTransaction:INSERT
role:membership:ADMIN ==> perm:coopAssetsTransaction:UPDATE
role:membership:AGENT ==> perm:coopAssetsTransaction:SELECT

```
