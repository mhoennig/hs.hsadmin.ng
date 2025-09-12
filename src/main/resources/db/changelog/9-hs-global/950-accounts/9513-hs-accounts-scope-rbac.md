### rbac profileContext

This code generated was by RbacViewMermaidFlowchartGenerator, do not amend manually.

```mermaid
%%{init:{'flowchart':{'htmlLabels':false}}}%%
flowchart TB

subgraph profileContext["`**profileContext**`"]
    direction TB
    style profileContext fill:#dd4901,stroke:#274d6e,stroke-width:8px

    subgraph profileContext:roles[ ]
        style profileContext:roles fill:#dd4901,stroke:white

        role:profileContext:OWNER[[profileContext:OWNER]]
        role:profileContext:ADMIN[[profileContext:ADMIN]]
        role:profileContext:REFERRER[[profileContext:REFERRER]]
    end

    subgraph profileContext:permissions[ ]
        style profileContext:permissions fill:#dd4901,stroke:white

        perm:profileContext:INSERT{{profileContext:INSERT}}
        perm:profileContext:UPDATE{{profileContext:UPDATE}}
        perm:profileContext:DELETE{{profileContext:DELETE}}
        perm:profileContext:SELECT{{profileContext:SELECT}}
    end
end

%% granting roles to roles
role:profileContext:OWNER ==> role:profileContext:ADMIN
role:profileContext:ADMIN ==> role:profileContext:REFERRER

%% granting permissions to roles
role:rbac.global:ADMIN ==> perm:profileContext:INSERT
role:rbac.global:ADMIN ==> perm:profileContext:UPDATE
role:rbac.global:ADMIN ==> perm:profileContext:DELETE
role:rbac.global:REFERRER ==> perm:profileContext:SELECT

```
