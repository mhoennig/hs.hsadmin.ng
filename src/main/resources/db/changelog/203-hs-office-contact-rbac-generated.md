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

        role:contact:owner[[contact:owner]]
        role:contact:admin[[contact:admin]]
        role:contact:referrer[[contact:referrer]]
    end

    subgraph contact:permissions[ ]
        style contact:permissions fill:#dd4901,stroke:white

        perm:contact:DELETE{{contact:DELETE}}
        perm:contact:UPDATE{{contact:UPDATE}}
        perm:contact:SELECT{{contact:SELECT}}
    end
end

%% granting roles to users
user:creator ==> role:contact:owner

%% granting roles to roles
role:global:admin ==> role:contact:owner
role:contact:owner ==> role:contact:admin
role:contact:admin ==> role:contact:referrer

%% granting permissions to roles
role:contact:owner ==> perm:contact:DELETE
role:contact:admin ==> perm:contact:UPDATE
role:contact:referrer ==> perm:contact:SELECT

```
