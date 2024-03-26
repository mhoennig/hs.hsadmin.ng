### rbac bankAccount

This code generated was by RbacViewMermaidFlowchartGenerator, do not amend manually.

```mermaid
%%{init:{'flowchart':{'htmlLabels':false}}}%%
flowchart TB

subgraph bankAccount["`**bankAccount**`"]
    direction TB
    style bankAccount fill:#dd4901,stroke:#274d6e,stroke-width:8px

    subgraph bankAccount:roles[ ]
        style bankAccount:roles fill:#dd4901,stroke:white

        role:bankAccount:owner[[bankAccount:owner]]
        role:bankAccount:admin[[bankAccount:admin]]
        role:bankAccount:referrer[[bankAccount:referrer]]
    end

    subgraph bankAccount:permissions[ ]
        style bankAccount:permissions fill:#dd4901,stroke:white

        perm:bankAccount:DELETE{{bankAccount:DELETE}}
        perm:bankAccount:UPDATE{{bankAccount:UPDATE}}
        perm:bankAccount:SELECT{{bankAccount:SELECT}}
    end
end

%% granting roles to users
user:creator ==> role:bankAccount:owner

%% granting roles to roles
role:global:admin ==> role:bankAccount:owner
role:bankAccount:owner ==> role:bankAccount:admin
role:bankAccount:admin ==> role:bankAccount:referrer

%% granting permissions to roles
role:bankAccount:owner ==> perm:bankAccount:DELETE
role:bankAccount:admin ==> perm:bankAccount:UPDATE
role:bankAccount:referrer ==> perm:bankAccount:SELECT

```
