### rbac asset inOtherCases

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
        role:asset:TENANT[[asset:TENANT]]
    end

    subgraph asset:permissions[ ]
        style asset:permissions fill:#dd4901,stroke:white

        perm:asset:INSERT{{asset:INSERT}}
        perm:asset:DELETE{{asset:DELETE}}
        perm:asset:UPDATE{{asset:UPDATE}}
        perm:asset:SELECT{{asset:SELECT}}
    end
end

subgraph bookingItem["`**bookingItem**`"]
    direction TB
    style bookingItem fill:#99bcdb,stroke:#274d6e,stroke-width:8px

    subgraph bookingItem:roles[ ]
        style bookingItem:roles fill:#99bcdb,stroke:white

        role:bookingItem:OWNER[[bookingItem:OWNER]]
        role:bookingItem:ADMIN[[bookingItem:ADMIN]]
        role:bookingItem:AGENT[[bookingItem:AGENT]]
        role:bookingItem:TENANT[[bookingItem:TENANT]]
    end
end

subgraph bookingItem.debitorRel["`**bookingItem.debitorRel**`"]
    direction TB
    style bookingItem.debitorRel fill:#99bcdb,stroke:#274d6e,stroke-width:8px

    subgraph bookingItem.debitorRel:roles[ ]
        style bookingItem.debitorRel:roles fill:#99bcdb,stroke:white

        role:bookingItem.debitorRel:OWNER[[bookingItem.debitorRel:OWNER]]
        role:bookingItem.debitorRel:ADMIN[[bookingItem.debitorRel:ADMIN]]
        role:bookingItem.debitorRel:AGENT[[bookingItem.debitorRel:AGENT]]
        role:bookingItem.debitorRel:TENANT[[bookingItem.debitorRel:TENANT]]
    end
end

subgraph parentServer["`**parentServer**`"]
    direction TB
    style parentServer fill:#99bcdb,stroke:#274d6e,stroke-width:8px

    subgraph parentServer:roles[ ]
        style parentServer:roles fill:#99bcdb,stroke:white

        role:parentServer:ADMIN[[parentServer:ADMIN]]
    end
end

%% granting roles to roles
role:global:ADMIN -.-> role:bookingItem.debitorRel:OWNER
role:bookingItem.debitorRel:OWNER -.-> role:bookingItem.debitorRel:ADMIN
role:bookingItem.debitorRel:ADMIN -.-> role:bookingItem.debitorRel:AGENT
role:bookingItem.debitorRel:AGENT -.-> role:bookingItem.debitorRel:TENANT
role:bookingItem.debitorRel:AGENT -.-> role:bookingItem:OWNER
role:bookingItem:OWNER -.-> role:bookingItem:ADMIN
role:bookingItem.debitorRel:AGENT -.-> role:bookingItem:ADMIN
role:bookingItem:ADMIN -.-> role:bookingItem:AGENT
role:bookingItem:AGENT -.-> role:bookingItem:TENANT
role:bookingItem:TENANT -.-> role:bookingItem.debitorRel:TENANT
role:bookingItem:ADMIN ==> role:asset:OWNER
role:asset:OWNER ==> role:asset:ADMIN
role:asset:ADMIN ==> role:asset:TENANT
role:asset:TENANT ==> role:bookingItem:TENANT

%% granting permissions to roles
role:asset:OWNER ==> perm:asset:DELETE
role:asset:ADMIN ==> perm:asset:UPDATE
role:asset:TENANT ==> perm:asset:SELECT

```
