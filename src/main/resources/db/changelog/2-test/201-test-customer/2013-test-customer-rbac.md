### rbac customer

This code generated was by RbacViewMermaidFlowchartGenerator, do not amend manually.

```mermaid
%%{init:{'flowchart':{'htmlLabels':false}}}%%
flowchart TB

subgraph customer["`**customer**`"]
    direction TB
    style customer fill:#dd4901,stroke:#274d6e,stroke-width:8px

    subgraph customer:roles[ ]
        style customer:roles fill:#dd4901,stroke:white

        role:customer:OWNER[[customer:OWNER]]
        role:customer:ADMIN[[customer:ADMIN]]
        role:customer:TENANT[[customer:TENANT]]
    end

    subgraph customer:permissions[ ]
        style customer:permissions fill:#dd4901,stroke:white

        perm:customer:INSERT{{customer:INSERT}}
        perm:customer:DELETE{{customer:DELETE}}
        perm:customer:UPDATE{{customer:UPDATE}}
        perm:customer:SELECT{{customer:SELECT}}
    end
end

%% granting roles to users
user:creator ==>|XX| role:customer:OWNER

%% granting roles to roles
role:global:ADMIN ==>|XX| role:customer:OWNER
role:customer:OWNER ==> role:customer:ADMIN
role:customer:ADMIN ==> role:customer:TENANT

%% granting permissions to roles
role:global:ADMIN ==> perm:customer:INSERT
role:customer:OWNER ==> perm:customer:DELETE
role:customer:ADMIN ==> perm:customer:UPDATE
role:customer:TENANT ==> perm:customer:SELECT

```
