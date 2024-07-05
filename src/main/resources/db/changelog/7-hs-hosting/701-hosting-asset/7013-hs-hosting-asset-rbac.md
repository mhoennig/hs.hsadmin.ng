### rbac asset

This code generated was by RbacViewMermaidFlowchartGenerator, do not amend manually.

```mermaid
%%{init:{'flowchart':{'htmlLabels':false}}}%%
flowchart TB

subgraph alarmContact["`**alarmContact**`"]
    direction TB
    style alarmContact fill:#99bcdb,stroke:#274d6e,stroke-width:8px

    subgraph alarmContact:roles[ ]
        style alarmContact:roles fill:#99bcdb,stroke:white

        role:alarmContact:OWNER[[alarmContact:OWNER]]
        role:alarmContact:ADMIN[[alarmContact:ADMIN]]
        role:alarmContact:REFERRER[[alarmContact:REFERRER]]
    end
end

subgraph asset["`**asset**`"]
    direction TB
    style asset fill:#dd4901,stroke:#274d6e,stroke-width:8px

    subgraph asset:roles[ ]
        style asset:roles fill:#dd4901,stroke:white

        role:asset:OWNER[[asset:OWNER]]
        role:asset:ADMIN[[asset:ADMIN]]
        role:asset:AGENT[[asset:AGENT]]
        role:asset:TENANT[[asset:TENANT]]
    end

    subgraph asset:permissions[ ]
        style asset:permissions fill:#dd4901,stroke:white

        perm:asset:INSERT{{asset:INSERT}}
        perm:asset:SELECT{{asset:SELECT}}
        perm:asset:DELETE{{asset:DELETE}}
        perm:asset:UPDATE{{asset:UPDATE}}
    end
end

subgraph assignedToAsset["`**assignedToAsset**`"]
    direction TB
    style assignedToAsset fill:#99bcdb,stroke:#274d6e,stroke-width:8px

    subgraph assignedToAsset:roles[ ]
        style assignedToAsset:roles fill:#99bcdb,stroke:white

        role:assignedToAsset:TENANT[[assignedToAsset:TENANT]]
    end
end

subgraph bookingItem["`**bookingItem**`"]
    direction TB
    style bookingItem fill:#99bcdb,stroke:#274d6e,stroke-width:8px

    subgraph bookingItem:roles[ ]
        style bookingItem:roles fill:#99bcdb,stroke:white

        role:bookingItem:OWNER[[bookingItem:OWNER]]
        role:bookingItem:ADMIN[[bookingItem:ADMIN]]
        role:bookingItem:AGENT[[bookingItem:AGENT]]
        role:bookingItem:TENANT[[bookingItem:TENANT]]
    end
end

subgraph parentAsset["`**parentAsset**`"]
    direction TB
    style parentAsset fill:#99bcdb,stroke:#274d6e,stroke-width:8px

    subgraph parentAsset:roles[ ]
        style parentAsset:roles fill:#99bcdb,stroke:white

        role:parentAsset:ADMIN[[parentAsset:ADMIN]]
        role:parentAsset:AGENT[[parentAsset:AGENT]]
        role:parentAsset:TENANT[[parentAsset:TENANT]]
    end
end

%% granting roles to roles
role:bookingItem:OWNER -.-> role:bookingItem:ADMIN
role:bookingItem:ADMIN -.-> role:bookingItem:AGENT
role:bookingItem:AGENT -.-> role:bookingItem:TENANT
role:global:ADMIN -.-> role:alarmContact:OWNER
role:alarmContact:OWNER -.-> role:alarmContact:ADMIN
role:alarmContact:ADMIN -.-> role:alarmContact:REFERRER
role:bookingItem:ADMIN ==> role:asset:OWNER
role:parentAsset:ADMIN ==> role:asset:OWNER
role:asset:OWNER ==> role:asset:ADMIN
role:bookingItem:AGENT ==> role:asset:ADMIN
role:parentAsset:AGENT ==> role:asset:ADMIN
role:asset:ADMIN ==> role:asset:AGENT
role:asset:AGENT ==> role:assignedToAsset:TENANT
role:asset:AGENT ==> role:alarmContact:REFERRER
role:asset:AGENT ==> role:asset:TENANT
role:asset:TENANT ==> role:bookingItem:TENANT
role:asset:TENANT ==> role:parentAsset:TENANT
role:alarmContact:ADMIN ==> role:asset:TENANT

%% granting permissions to roles
role:global:ADMIN ==> perm:asset:INSERT
role:parentAsset:ADMIN ==> perm:asset:INSERT
role:global:GUEST ==> perm:asset:INSERT
role:global:ADMIN ==> perm:asset:SELECT
role:asset:OWNER ==> perm:asset:DELETE
role:asset:ADMIN ==> perm:asset:UPDATE
role:asset:TENANT ==> perm:asset:SELECT

```
