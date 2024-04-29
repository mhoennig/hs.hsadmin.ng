### rbac domain

This code generated was by RbacViewMermaidFlowchartGenerator, do not amend manually.

```mermaid
%%{init:{'flowchart':{'htmlLabels':false}}}%%
flowchart TB

subgraph domain["`**domain**`"]
    direction TB
    style domain fill:#dd4901,stroke:#274d6e,stroke-width:8px

    subgraph domain:roles[ ]
        style domain:roles fill:#dd4901,stroke:white

        role:domain:OWNER[[domain:OWNER]]
        role:domain:ADMIN[[domain:ADMIN]]
    end

    subgraph domain:permissions[ ]
        style domain:permissions fill:#dd4901,stroke:white

        perm:domain:INSERT{{domain:INSERT}}
        perm:domain:DELETE{{domain:DELETE}}
        perm:domain:UPDATE{{domain:UPDATE}}
        perm:domain:SELECT{{domain:SELECT}}
    end
end

subgraph package["`**package**`"]
    direction TB
    style package fill:#99bcdb,stroke:#274d6e,stroke-width:8px

    subgraph package:roles[ ]
        style package:roles fill:#99bcdb,stroke:white

        role:package:OWNER[[package:OWNER]]
        role:package:ADMIN[[package:ADMIN]]
        role:package:TENANT[[package:TENANT]]
    end
end

subgraph package.customer["`**package.customer**`"]
    direction TB
    style package.customer fill:#99bcdb,stroke:#274d6e,stroke-width:8px

    subgraph package.customer:roles[ ]
        style package.customer:roles fill:#99bcdb,stroke:white

        role:package.customer:OWNER[[package.customer:OWNER]]
        role:package.customer:ADMIN[[package.customer:ADMIN]]
        role:package.customer:TENANT[[package.customer:TENANT]]
    end
end

%% granting roles to roles
role:global:ADMIN -.->|XX| role:package.customer:OWNER
role:package.customer:OWNER -.-> role:package.customer:ADMIN
role:package.customer:ADMIN -.-> role:package.customer:TENANT
role:package.customer:ADMIN -.-> role:package:OWNER
role:package:OWNER -.-> role:package:ADMIN
role:package:ADMIN -.-> role:package:TENANT
role:package:TENANT -.-> role:package.customer:TENANT
role:package:ADMIN ==> role:domain:OWNER
role:domain:OWNER ==> role:package:TENANT
role:domain:OWNER ==> role:domain:ADMIN
role:domain:ADMIN ==> role:package:TENANT

%% granting permissions to roles
role:package:ADMIN ==> perm:domain:INSERT
role:domain:OWNER ==> perm:domain:DELETE
role:domain:OWNER ==> perm:domain:UPDATE
role:domain:ADMIN ==> perm:domain:SELECT

```
