### rbac domain

This code generated was by RbacViewMermaidFlowchartGenerator, do not amend manually.

```mermaid
%%{init:{'flowchart':{'htmlLabels':false}}}%%
flowchart TB

subgraph package.customer["`**package.customer**`"]
    direction TB
    style package.customer fill:#99bcdb,stroke:#274d6e,stroke-width:8px

    subgraph package.customer:roles[ ]
        style package.customer:roles fill:#99bcdb,stroke:white

        role:package.customer:owner[[package.customer:owner]]
        role:package.customer:admin[[package.customer:admin]]
        role:package.customer:tenant[[package.customer:tenant]]
    end
end

subgraph package["`**package**`"]
    direction TB
    style package fill:#99bcdb,stroke:#274d6e,stroke-width:8px

    subgraph package.customer["`**package.customer**`"]
        direction TB
        style package.customer fill:#99bcdb,stroke:#274d6e,stroke-width:8px

        subgraph package.customer:roles[ ]
            style package.customer:roles fill:#99bcdb,stroke:white

            role:package.customer:owner[[package.customer:owner]]
            role:package.customer:admin[[package.customer:admin]]
            role:package.customer:tenant[[package.customer:tenant]]
        end
    end

    subgraph package:roles[ ]
        style package:roles fill:#99bcdb,stroke:white

        role:package:owner[[package:owner]]
        role:package:admin[[package:admin]]
        role:package:tenant[[package:tenant]]
    end
end

subgraph domain["`**domain**`"]
    direction TB
    style domain fill:#dd4901,stroke:#274d6e,stroke-width:8px

    subgraph domain:roles[ ]
        style domain:roles fill:#dd4901,stroke:white

        role:domain:owner[[domain:owner]]
        role:domain:admin[[domain:admin]]
    end

    subgraph domain:permissions[ ]
        style domain:permissions fill:#dd4901,stroke:white

        perm:domain:INSERT{{domain:INSERT}}
        perm:domain:DELETE{{domain:DELETE}}
        perm:domain:UPDATE{{domain:UPDATE}}
        perm:domain:SELECT{{domain:SELECT}}
    end
end

%% granting roles to roles
role:global:admin -.->|XX| role:package.customer:owner
role:package.customer:owner -.-> role:package.customer:admin
role:package.customer:admin -.-> role:package.customer:tenant
role:package.customer:admin -.-> role:package:owner
role:package:owner -.-> role:package:admin
role:package:admin -.-> role:package:tenant
role:package:tenant -.-> role:package.customer:tenant
role:package:admin ==> role:domain:owner
role:domain:owner ==> role:package:tenant
role:domain:owner ==> role:domain:admin
role:domain:admin ==> role:package:tenant

%% granting permissions to roles
role:package:admin ==> perm:domain:INSERT
role:domain:owner ==> perm:domain:DELETE
role:domain:owner ==> perm:domain:UPDATE
role:domain:admin ==> perm:domain:SELECT

```
