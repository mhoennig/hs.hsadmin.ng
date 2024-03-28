### rbac sepaMandate

This code generated was by RbacViewMermaidFlowchartGenerator, do not amend manually.

```mermaid
%%{init:{'flowchart':{'htmlLabels':false}}}%%
flowchart TB

subgraph bankAccount["`**bankAccount**`"]
    direction TB
    style bankAccount fill:#99bcdb,stroke:#274d6e,stroke-width:8px

    subgraph bankAccount:roles[ ]
        style bankAccount:roles fill:#99bcdb,stroke:white

        role:bankAccount:owner[[bankAccount:owner]]
        role:bankAccount:admin[[bankAccount:admin]]
        role:bankAccount:referrer[[bankAccount:referrer]]
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

subgraph sepaMandate["`**sepaMandate**`"]
    direction TB
    style sepaMandate fill:#dd4901,stroke:#274d6e,stroke-width:8px

    subgraph sepaMandate:roles[ ]
        style sepaMandate:roles fill:#dd4901,stroke:white

        role:sepaMandate:owner[[sepaMandate:owner]]
        role:sepaMandate:admin[[sepaMandate:admin]]
        role:sepaMandate:agent[[sepaMandate:agent]]
        role:sepaMandate:referrer[[sepaMandate:referrer]]
    end

    subgraph sepaMandate:permissions[ ]
        style sepaMandate:permissions fill:#dd4901,stroke:white

        perm:sepaMandate:DELETE{{sepaMandate:DELETE}}
        perm:sepaMandate:UPDATE{{sepaMandate:UPDATE}}
        perm:sepaMandate:SELECT{{sepaMandate:SELECT}}
        perm:sepaMandate:INSERT{{sepaMandate:INSERT}}
    end
end

subgraph debitorRel["`**debitorRel**`"]
    direction TB
    style debitorRel fill:#99bcdb,stroke:#274d6e,stroke-width:8px

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

    subgraph debitorRel:roles[ ]
        style debitorRel:roles fill:#99bcdb,stroke:white

        role:debitorRel:owner[[debitorRel:owner]]
        role:debitorRel:admin[[debitorRel:admin]]
        role:debitorRel:agent[[debitorRel:agent]]
        role:debitorRel:tenant[[debitorRel:tenant]]
    end
end

%% granting roles to users
user:creator ==> role:sepaMandate:owner

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
role:global:admin -.-> role:bankAccount:owner
role:bankAccount:owner -.-> role:bankAccount:admin
role:bankAccount:admin -.-> role:bankAccount:referrer
role:global:admin ==> role:sepaMandate:owner
role:sepaMandate:owner ==> role:sepaMandate:admin
role:sepaMandate:admin ==> role:sepaMandate:agent
role:sepaMandate:agent ==> role:bankAccount:referrer
role:sepaMandate:agent ==> role:debitorRel:agent
role:sepaMandate:agent ==> role:sepaMandate:referrer
role:bankAccount:admin ==> role:sepaMandate:referrer
role:debitorRel:agent ==> role:sepaMandate:referrer
role:sepaMandate:referrer ==> role:debitorRel:tenant

%% granting permissions to roles
role:sepaMandate:owner ==> perm:sepaMandate:DELETE
role:sepaMandate:admin ==> perm:sepaMandate:UPDATE
role:sepaMandate:referrer ==> perm:sepaMandate:SELECT
role:debitorRel:admin ==> perm:sepaMandate:INSERT

```
