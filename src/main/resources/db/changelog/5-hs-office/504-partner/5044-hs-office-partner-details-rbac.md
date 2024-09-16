### rbac partnerDetails

This code generated was by RbacViewMermaidFlowchartGenerator, do not amend manually.

```mermaid
%%{init:{'flowchart':{'htmlLabels':false}}}%%
flowchart TB

subgraph partnerDetails["`**partnerDetails**`"]
    direction TB
    style partnerDetails fill:#dd4901,stroke:#274d6e,stroke-width:8px

    subgraph partnerDetails:permissions[ ]
        style partnerDetails:permissions fill:#dd4901,stroke:white

        perm:partnerDetails:INSERT{{partnerDetails:INSERT}}
    end
end

%% granting permissions to roles
role:rbac.global:ADMIN ==> perm:partnerDetails:INSERT

```
