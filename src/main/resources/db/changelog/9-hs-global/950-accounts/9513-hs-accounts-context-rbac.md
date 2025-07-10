### rbac credentialsContext

This code generated was by RbacViewMermaidFlowchartGenerator, do not amend manually.

```mermaid
%%{init:{'flowchart':{'htmlLabels':false}}}%%
flowchart TB

subgraph credentialsContext["`**credentialsContext**`"]
    direction TB
    style credentialsContext fill:#dd4901,stroke:#274d6e,stroke-width:8px

    subgraph credentialsContext:roles[ ]
        style credentialsContext:roles fill:#dd4901,stroke:white

        role:credentialsContext:OWNER[[credentialsContext:OWNER]]
        role:credentialsContext:ADMIN[[credentialsContext:ADMIN]]
        role:credentialsContext:REFERRER[[credentialsContext:REFERRER]]
    end

    subgraph credentialsContext:permissions[ ]
        style credentialsContext:permissions fill:#dd4901,stroke:white

        perm:credentialsContext:INSERT{{credentialsContext:INSERT}}
        perm:credentialsContext:UPDATE{{credentialsContext:UPDATE}}
        perm:credentialsContext:DELETE{{credentialsContext:DELETE}}
        perm:credentialsContext:SELECT{{credentialsContext:SELECT}}
    end
end

%% granting roles to roles
role:credentialsContext:OWNER ==> role:credentialsContext:ADMIN
role:credentialsContext:ADMIN ==> role:credentialsContext:REFERRER

%% granting permissions to roles
role:rbac.global:ADMIN ==> perm:credentialsContext:INSERT
role:rbac.global:ADMIN ==> perm:credentialsContext:UPDATE
role:rbac.global:ADMIN ==> perm:credentialsContext:DELETE
role:rbac.global:REFERRER ==> perm:credentialsContext:SELECT

```
