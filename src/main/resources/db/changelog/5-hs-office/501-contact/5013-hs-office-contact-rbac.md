### rbac contact

This code generated was by RbacViewMermaidFlowchartGenerator, do not amend manually.

```mermaid
%%{init:{'flowchart':{'htmlLabels':false}}}%%
flowchart TB

subgraph contact["`**contact**`"]
    direction TB
    style contact fill:#dd4901,stroke:#274d6e,stroke-width:8px

    subgraph contact:roles[ ]
        style contact:roles fill:#dd4901,stroke:white

        role:contact:OWNER[[contact:OWNER]]
        role:contact:ADMIN[[contact:ADMIN]]
        role:contact:REFERRER[[contact:REFERRER]]
    end

    subgraph contact:permissions[ ]
        style contact:permissions fill:#dd4901,stroke:white

        perm:contact:DELETE{{contact:DELETE}}
        perm:contact:UPDATE{{contact:UPDATE}}
        perm:contact:SELECT{{contact:SELECT}}
        perm:contact:INSERT{{contact:INSERT}}
    end
end

%% granting roles to users
user:creator ==> role:contact:OWNER

%% granting roles to roles
role:global:ADMIN ==> role:contact:OWNER
role:contact:OWNER ==> role:contact:ADMIN
role:contact:ADMIN ==> role:contact:REFERRER

%% granting permissions to roles
role:contact:OWNER ==> perm:contact:DELETE
role:contact:ADMIN ==> perm:contact:UPDATE
role:contact:REFERRER ==> perm:contact:SELECT
role:global:GUEST ==> perm:contact:INSERT

```
