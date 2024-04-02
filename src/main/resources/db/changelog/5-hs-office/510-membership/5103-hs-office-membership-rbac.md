### rbac membership

This code generated was by RbacViewMermaidFlowchartGenerator, do not amend manually.

```mermaid
%%{init:{'flowchart':{'htmlLabels':false}}}%%
flowchart TB

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

subgraph membership["`**membership**`"]
    direction TB
    style membership fill:#dd4901,stroke:#274d6e,stroke-width:8px

    subgraph membership:roles[ ]
        style membership:roles fill:#dd4901,stroke:white

        role:membership:OWNER[[membership:OWNER]]
        role:membership:ADMIN[[membership:ADMIN]]
        role:membership:AGENT[[membership:AGENT]]
    end

    subgraph membership:permissions[ ]
        style membership:permissions fill:#dd4901,stroke:white

        perm:membership:INSERT{{membership:INSERT}}
        perm:membership:DELETE{{membership:DELETE}}
        perm:membership:UPDATE{{membership:UPDATE}}
        perm:membership:SELECT{{membership:SELECT}}
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

%% granting roles to users
user:creator ==> role:membership:OWNER

%% granting roles to roles
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
role:membership:OWNER ==> role:membership:ADMIN
role:partnerRel:ADMIN ==> role:membership:ADMIN
role:membership:ADMIN ==> role:membership:AGENT
role:partnerRel:AGENT ==> role:membership:AGENT
role:membership:AGENT ==> role:partnerRel:TENANT

%% granting permissions to roles
role:global:ADMIN ==> perm:membership:INSERT
role:membership:ADMIN ==> perm:membership:DELETE
role:membership:ADMIN ==> perm:membership:UPDATE
role:membership:AGENT ==> perm:membership:SELECT

```
