### rbac bookingItem

This code generated was by RbacViewMermaidFlowchartGenerator, do not amend manually.

```mermaid
%%{init:{'flowchart':{'htmlLabels':false}}}%%
flowchart TB

subgraph bookingItem["`**bookingItem**`"]
    direction TB
    style bookingItem fill:#dd4901,stroke:#274d6e,stroke-width:8px

    subgraph bookingItem:roles[ ]
        style bookingItem:roles fill:#dd4901,stroke:white

        role:bookingItem:OWNER[[bookingItem:OWNER]]
        role:bookingItem:ADMIN[[bookingItem:ADMIN]]
        role:bookingItem:AGENT[[bookingItem:AGENT]]
        role:bookingItem:TENANT[[bookingItem:TENANT]]
    end

    subgraph bookingItem:permissions[ ]
        style bookingItem:permissions fill:#dd4901,stroke:white

        perm:bookingItem:INSERT{{bookingItem:INSERT}}
        perm:bookingItem:DELETE{{bookingItem:DELETE}}
        perm:bookingItem:UPDATE{{bookingItem:UPDATE}}
        perm:bookingItem:SELECT{{bookingItem:SELECT}}
    end
end

subgraph debitorRel["`**debitorRel**`"]
    direction TB
    style debitorRel fill:#99bcdb,stroke:#274d6e,stroke-width:8px

    subgraph debitorRel:roles[ ]
        style debitorRel:roles fill:#99bcdb,stroke:white

        role:debitorRel:OWNER[[debitorRel:OWNER]]
        role:debitorRel:ADMIN[[debitorRel:ADMIN]]
        role:debitorRel:AGENT[[debitorRel:AGENT]]
        role:debitorRel:TENANT[[debitorRel:TENANT]]
    end
end

%% granting roles to roles
role:global:ADMIN -.-> role:debitorRel:OWNER
role:debitorRel:OWNER -.-> role:debitorRel:ADMIN
role:debitorRel:ADMIN -.-> role:debitorRel:AGENT
role:debitorRel:AGENT -.-> role:debitorRel:TENANT
role:debitorRel:AGENT ==> role:bookingItem:OWNER
role:bookingItem:OWNER ==> role:bookingItem:ADMIN
role:debitorRel:AGENT ==> role:bookingItem:ADMIN
role:bookingItem:ADMIN ==> role:bookingItem:AGENT
role:bookingItem:AGENT ==> role:bookingItem:TENANT
role:bookingItem:TENANT ==> role:debitorRel:TENANT

%% granting permissions to roles
role:debitorRel:ADMIN ==> perm:bookingItem:INSERT
role:global:ADMIN ==> perm:bookingItem:DELETE
role:bookingItem:ADMIN ==> perm:bookingItem:UPDATE
role:bookingItem:TENANT ==> perm:bookingItem:SELECT

```
