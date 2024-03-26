### rbac relation

This code generated was by RbacViewMermaidFlowchartGenerator, do not amend manually.

```mermaid
%%{init:{'flowchart':{'htmlLabels':false}}}%%
flowchart TB

subgraph holderPerson["`**holderPerson**`"]
    direction TB
    style holderPerson fill:#99bcdb,stroke:#274d6e,stroke-width:8px

    subgraph holderPerson:roles[ ]
        style holderPerson:roles fill:#99bcdb,stroke:white

        role:holderPerson:owner[[holderPerson:owner]]
        role:holderPerson:admin[[holderPerson:admin]]
        role:holderPerson:referrer[[holderPerson:referrer]]
    end
end

subgraph anchorPerson["`**anchorPerson**`"]
    direction TB
    style anchorPerson fill:#99bcdb,stroke:#274d6e,stroke-width:8px

    subgraph anchorPerson:roles[ ]
        style anchorPerson:roles fill:#99bcdb,stroke:white

        role:anchorPerson:owner[[anchorPerson:owner]]
        role:anchorPerson:admin[[anchorPerson:admin]]
        role:anchorPerson:referrer[[anchorPerson:referrer]]
    end
end

subgraph contact["`**contact**`"]
    direction TB
    style contact fill:#99bcdb,stroke:#274d6e,stroke-width:8px

    subgraph contact:roles[ ]
        style contact:roles fill:#99bcdb,stroke:white

        role:contact:owner[[contact:owner]]
        role:contact:admin[[contact:admin]]
        role:contact:referrer[[contact:referrer]]
    end
end

subgraph relation["`**relation**`"]
    direction TB
    style relation fill:#dd4901,stroke:#274d6e,stroke-width:8px

    subgraph relation:roles[ ]
        style relation:roles fill:#dd4901,stroke:white

        role:relation:owner[[relation:owner]]
        role:relation:admin[[relation:admin]]
        role:relation:agent[[relation:agent]]
        role:relation:tenant[[relation:tenant]]
    end

    subgraph relation:permissions[ ]
        style relation:permissions fill:#dd4901,stroke:white

        perm:relation:DELETE{{relation:DELETE}}
        perm:relation:UPDATE{{relation:UPDATE}}
        perm:relation:SELECT{{relation:SELECT}}
    end
end

%% granting roles to users
user:creator ==> role:relation:owner

%% granting roles to roles
role:global:admin -.-> role:anchorPerson:owner
role:anchorPerson:owner -.-> role:anchorPerson:admin
role:anchorPerson:admin -.-> role:anchorPerson:referrer
role:global:admin -.-> role:holderPerson:owner
role:holderPerson:owner -.-> role:holderPerson:admin
role:holderPerson:admin -.-> role:holderPerson:referrer
role:global:admin -.-> role:contact:owner
role:contact:owner -.-> role:contact:admin
role:contact:admin -.-> role:contact:referrer
role:global:admin ==> role:relation:owner
role:relation:owner ==> role:relation:admin
role:anchorPerson:admin ==> role:relation:admin
role:relation:admin ==> role:relation:agent
role:holderPerson:admin ==> role:relation:agent
role:relation:agent ==> role:relation:tenant
role:holderPerson:admin ==> role:relation:tenant
role:contact:admin ==> role:relation:tenant
role:relation:tenant ==> role:anchorPerson:referrer
role:relation:tenant ==> role:holderPerson:referrer
role:relation:tenant ==> role:contact:referrer

%% granting permissions to roles
role:relation:owner ==> perm:relation:DELETE
role:relation:admin ==> perm:relation:UPDATE
role:relation:tenant ==> perm:relation:SELECT

```
