### rbac package

This code generated was by RbacViewMermaidFlowchartGenerator, do not amend manually.

```mermaid
%%{init:{'flowchart':{'htmlLabels':false}}}%%
flowchart TB

subgraph package["`**package**`"]
    direction TB
    style package fill:#dd4901,stroke:#274d6e,stroke-width:8px

    subgraph package:roles[ ]
        style package:roles fill:#dd4901,stroke:white

        role:package:OWNER[[package:OWNER]]
        role:package:ADMIN[[package:ADMIN]]
        role:package:TENANT[[package:TENANT]]
    end

    subgraph package:permissions[ ]
        style package:permissions fill:#dd4901,stroke:white

        perm:package:INSERT{{package:INSERT}}
        perm:package:DELETE{{package:DELETE}}
        perm:package:UPDATE{{package:UPDATE}}
        perm:package:SELECT{{package:SELECT}}
    end
end

subgraph customer["`**customer**`"]
    direction TB
    style customer fill:#99bcdb,stroke:#274d6e,stroke-width:8px

    subgraph customer:roles[ ]
        style customer:roles fill:#99bcdb,stroke:white

        role:customer:OWNER[[customer:OWNER]]
        role:customer:ADMIN[[customer:ADMIN]]
        role:customer:TENANT[[customer:TENANT]]
    end
end

%% granting roles to roles
role:global:ADMIN -.->|XX| role:customer:OWNER
role:customer:OWNER -.-> role:customer:ADMIN
role:customer:ADMIN -.-> role:customer:TENANT
role:customer:ADMIN ==> role:package:OWNER
role:package:OWNER ==> role:package:ADMIN
role:package:ADMIN ==> role:package:TENANT
role:package:TENANT ==> role:customer:TENANT

%% granting permissions to roles
role:customer:ADMIN ==> perm:package:INSERT
role:package:OWNER ==> perm:package:DELETE
role:package:OWNER ==> perm:package:UPDATE
role:package:TENANT ==> perm:package:SELECT

```
