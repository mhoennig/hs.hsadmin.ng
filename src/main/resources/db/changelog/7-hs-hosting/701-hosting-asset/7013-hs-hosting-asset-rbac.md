### rbac asset

This code generated was by RbacViewMermaidFlowchartGenerator, do not amend manually.

```mermaid
%%{init:{'flowchart':{'htmlLabels':false}}}%%
flowchart TB

subgraph asset["`**asset**`"]
    direction TB
    style asset fill:#dd4901,stroke:#274d6e,stroke-width:8px

    subgraph asset:roles[ ]
        style asset:roles fill:#dd4901,stroke:white

        role:asset:OWNER[[asset:OWNER]]
        role:asset:ADMIN[[asset:ADMIN]]
        role:asset:AGENT[[asset:AGENT]]
        role:asset:TENANT[[asset:TENANT]]
    end

    subgraph asset:permissions[ ]
        style asset:permissions fill:#dd4901,stroke:white

        perm:asset:INSERT{{asset:INSERT}}
        perm:asset:DELETE{{asset:DELETE}}
        perm:asset:UPDATE{{asset:UPDATE}}
    end
end

subgraph assignedToAsset["`**assignedToAsset**`"]
    direction TB
    style assignedToAsset fill:#99bcdb,stroke:#274d6e,stroke-width:8px

    subgraph assignedToAsset:roles[ ]
        style assignedToAsset:roles fill:#99bcdb,stroke:white

        role:assignedToAsset:TENANT[[assignedToAsset:TENANT]]
    end
end

%% granting roles to roles
role:asset:OWNER ==> role:asset:ADMIN
role:asset:ADMIN ==> role:asset:AGENT
role:asset:AGENT ==> role:assignedToAsset:TENANT
role:asset:AGENT ==> role:asset:TENANT
role:assignedToAsset:TENANT ==> role:asset:TENANT

%% granting permissions to roles
role:global:ADMIN ==> perm:asset:INSERT
role:asset:OWNER ==> perm:asset:DELETE
role:asset:ADMIN ==> perm:asset:UPDATE

```
