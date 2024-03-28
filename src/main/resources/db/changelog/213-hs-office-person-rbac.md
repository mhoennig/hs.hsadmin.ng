### rbac person

This code generated was by RbacViewMermaidFlowchartGenerator, do not amend manually.

```mermaid
%%{init:{'flowchart':{'htmlLabels':false}}}%%
flowchart TB

subgraph person["`**person**`"]
    direction TB
    style person fill:#dd4901,stroke:#274d6e,stroke-width:8px

    subgraph person:roles[ ]
        style person:roles fill:#dd4901,stroke:white

        role:person:owner[[person:owner]]
        role:person:admin[[person:admin]]
        role:person:referrer[[person:referrer]]
    end

    subgraph person:permissions[ ]
        style person:permissions fill:#dd4901,stroke:white

        perm:person:INSERT{{person:INSERT}}
        perm:person:DELETE{{person:DELETE}}
        perm:person:UPDATE{{person:UPDATE}}
        perm:person:SELECT{{person:SELECT}}
    end
end

%% granting roles to users
user:creator ==> role:person:owner

%% granting roles to roles
role:global:admin ==> role:person:owner
role:person:owner ==> role:person:admin
role:person:admin ==> role:person:referrer

%% granting permissions to roles
role:global:guest ==> perm:person:INSERT
role:person:owner ==> perm:person:DELETE
role:person:admin ==> perm:person:UPDATE
role:person:referrer ==> perm:person:SELECT

```
