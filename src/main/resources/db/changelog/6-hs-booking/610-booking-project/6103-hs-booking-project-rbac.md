### rbac project

This code generated was by RbacViewMermaidFlowchartGenerator, do not amend manually.

```mermaid
%%{init:{'flowchart':{'htmlLabels':false}}}%%
flowchart TB

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

subgraph project["`**project**`"]
    direction TB
    style project fill:#dd4901,stroke:#274d6e,stroke-width:8px

    subgraph project:roles[ ]
        style project:roles fill:#dd4901,stroke:white

        role:project:OWNER[[project:OWNER]]
        role:project:ADMIN[[project:ADMIN]]
        role:project:AGENT[[project:AGENT]]
        role:project:TENANT[[project:TENANT]]
    end

    subgraph project:permissions[ ]
        style project:permissions fill:#dd4901,stroke:white

        perm:project:INSERT{{project:INSERT}}
        perm:project:DELETE{{project:DELETE}}
        perm:project:UPDATE{{project:UPDATE}}
        perm:project:SELECT{{project:SELECT}}
    end
end

%% granting roles to roles
role:global:ADMIN -.-> role:debitorRel:OWNER
role:debitorRel:OWNER -.-> role:debitorRel:ADMIN
role:debitorRel:ADMIN -.-> role:debitorRel:AGENT
role:debitorRel:AGENT -.-> role:debitorRel:TENANT
role:debitorRel:AGENT ==> role:project:OWNER
role:project:OWNER ==> role:project:ADMIN
role:project:ADMIN ==> role:project:AGENT
role:project:AGENT ==> role:project:TENANT
role:project:TENANT ==> role:debitorRel:TENANT

%% granting permissions to roles
role:debitorRel:ADMIN ==> perm:project:INSERT
role:global:ADMIN ==> perm:project:DELETE
role:project:ADMIN ==> perm:project:UPDATE
role:project:TENANT ==> perm:project:SELECT

```
