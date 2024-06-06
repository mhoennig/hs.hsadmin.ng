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

subgraph project["`**project**`"]
    direction TB
    style project fill:#99bcdb,stroke:#274d6e,stroke-width:8px

    subgraph project:roles[ ]
        style project:roles fill:#99bcdb,stroke:white

        role:project:OWNER[[project:OWNER]]
        role:project:ADMIN[[project:ADMIN]]
        role:project:AGENT[[project:AGENT]]
        role:project:TENANT[[project:TENANT]]
    end
end

%% granting roles to roles
role:project:OWNER -.-> role:project:ADMIN
role:project:ADMIN -.-> role:project:AGENT
role:project:AGENT -.-> role:project:TENANT
role:project:AGENT ==> role:bookingItem:OWNER
role:bookingItem:OWNER ==> role:bookingItem:ADMIN
role:bookingItem:ADMIN ==> role:bookingItem:AGENT
role:bookingItem:AGENT ==> role:bookingItem:TENANT
role:bookingItem:TENANT ==> role:project:TENANT

%% granting permissions to roles
role:global:ADMIN ==> perm:bookingItem:INSERT
role:global:ADMIN ==> perm:bookingItem:DELETE
role:project:ADMIN ==> perm:bookingItem:INSERT
role:bookingItem:ADMIN ==> perm:bookingItem:UPDATE
role:bookingItem:TENANT ==> perm:bookingItem:SELECT

```
