### rbac asset inCaseOf:MANAGED_SERVER

This code generated was by RbacViewMermaidFlowchartGenerator, do not amend manually.

```mermaid
%%{init:{'flowchart':{'htmlLabels':false}}}%%
flowchart TB

subgraph parentServer.bookingItem["`**parentServer.bookingItem**`"]
    direction TB
    style parentServer.bookingItem fill:#99bcdb,stroke:#274d6e,stroke-width:8px

    subgraph parentServer.bookingItem:roles[ ]
        style parentServer.bookingItem:roles fill:#99bcdb,stroke:white

        role:parentServer.bookingItem:OWNER[[parentServer.bookingItem:OWNER]]
        role:parentServer.bookingItem:ADMIN[[parentServer.bookingItem:ADMIN]]
        role:parentServer.bookingItem:AGENT[[parentServer.bookingItem:AGENT]]
        role:parentServer.bookingItem:TENANT[[parentServer.bookingItem:TENANT]]
    end
end

subgraph parentServer.bookingItem.debitorRel.anchorPerson["`**parentServer.bookingItem.debitorRel.anchorPerson**`"]
    direction TB
    style parentServer.bookingItem.debitorRel.anchorPerson fill:#99bcdb,stroke:#274d6e,stroke-width:8px

    subgraph parentServer.bookingItem.debitorRel.anchorPerson:roles[ ]
        style parentServer.bookingItem.debitorRel.anchorPerson:roles fill:#99bcdb,stroke:white

        role:parentServer.bookingItem.debitorRel.anchorPerson:OWNER[[parentServer.bookingItem.debitorRel.anchorPerson:OWNER]]
        role:parentServer.bookingItem.debitorRel.anchorPerson:ADMIN[[parentServer.bookingItem.debitorRel.anchorPerson:ADMIN]]
        role:parentServer.bookingItem.debitorRel.anchorPerson:REFERRER[[parentServer.bookingItem.debitorRel.anchorPerson:REFERRER]]
    end
end

subgraph parentServer.bookingItem.debitorRel.holderPerson["`**parentServer.bookingItem.debitorRel.holderPerson**`"]
    direction TB
    style parentServer.bookingItem.debitorRel.holderPerson fill:#99bcdb,stroke:#274d6e,stroke-width:8px

    subgraph parentServer.bookingItem.debitorRel.holderPerson:roles[ ]
        style parentServer.bookingItem.debitorRel.holderPerson:roles fill:#99bcdb,stroke:white

        role:parentServer.bookingItem.debitorRel.holderPerson:OWNER[[parentServer.bookingItem.debitorRel.holderPerson:OWNER]]
        role:parentServer.bookingItem.debitorRel.holderPerson:ADMIN[[parentServer.bookingItem.debitorRel.holderPerson:ADMIN]]
        role:parentServer.bookingItem.debitorRel.holderPerson:REFERRER[[parentServer.bookingItem.debitorRel.holderPerson:REFERRER]]
    end
end

subgraph parentServer["`**parentServer**`"]
    direction TB
    style parentServer fill:#99bcdb,stroke:#274d6e,stroke-width:8px
end

subgraph parentServer.bookingItem.debitor.partnerRel.holderPerson["`**parentServer.bookingItem.debitor.partnerRel.holderPerson**`"]
    direction TB
    style parentServer.bookingItem.debitor.partnerRel.holderPerson fill:#99bcdb,stroke:#274d6e,stroke-width:8px

    subgraph parentServer.bookingItem.debitor.partnerRel.holderPerson:roles[ ]
        style parentServer.bookingItem.debitor.partnerRel.holderPerson:roles fill:#99bcdb,stroke:white

        role:parentServer.bookingItem.debitor.partnerRel.holderPerson:OWNER[[parentServer.bookingItem.debitor.partnerRel.holderPerson:OWNER]]
        role:parentServer.bookingItem.debitor.partnerRel.holderPerson:ADMIN[[parentServer.bookingItem.debitor.partnerRel.holderPerson:ADMIN]]
        role:parentServer.bookingItem.debitor.partnerRel.holderPerson:REFERRER[[parentServer.bookingItem.debitor.partnerRel.holderPerson:REFERRER]]
    end
end

subgraph parentServer.bookingItem.debitor.partnerRel.anchorPerson["`**parentServer.bookingItem.debitor.partnerRel.anchorPerson**`"]
    direction TB
    style parentServer.bookingItem.debitor.partnerRel.anchorPerson fill:#99bcdb,stroke:#274d6e,stroke-width:8px

    subgraph parentServer.bookingItem.debitor.partnerRel.anchorPerson:roles[ ]
        style parentServer.bookingItem.debitor.partnerRel.anchorPerson:roles fill:#99bcdb,stroke:white

        role:parentServer.bookingItem.debitor.partnerRel.anchorPerson:OWNER[[parentServer.bookingItem.debitor.partnerRel.anchorPerson:OWNER]]
        role:parentServer.bookingItem.debitor.partnerRel.anchorPerson:ADMIN[[parentServer.bookingItem.debitor.partnerRel.anchorPerson:ADMIN]]
        role:parentServer.bookingItem.debitor.partnerRel.anchorPerson:REFERRER[[parentServer.bookingItem.debitor.partnerRel.anchorPerson:REFERRER]]
    end
end

subgraph bookingItem.debitor.debitorRel.anchorPerson["`**bookingItem.debitor.debitorRel.anchorPerson**`"]
    direction TB
    style bookingItem.debitor.debitorRel.anchorPerson fill:#99bcdb,stroke:#274d6e,stroke-width:8px

    subgraph bookingItem.debitor.debitorRel.anchorPerson:roles[ ]
        style bookingItem.debitor.debitorRel.anchorPerson:roles fill:#99bcdb,stroke:white

        role:bookingItem.debitor.debitorRel.anchorPerson:OWNER[[bookingItem.debitor.debitorRel.anchorPerson:OWNER]]
        role:bookingItem.debitor.debitorRel.anchorPerson:ADMIN[[bookingItem.debitor.debitorRel.anchorPerson:ADMIN]]
        role:bookingItem.debitor.debitorRel.anchorPerson:REFERRER[[bookingItem.debitor.debitorRel.anchorPerson:REFERRER]]
    end
end

subgraph parentServer.bookingItem.debitorRel.contact["`**parentServer.bookingItem.debitorRel.contact**`"]
    direction TB
    style parentServer.bookingItem.debitorRel.contact fill:#99bcdb,stroke:#274d6e,stroke-width:8px

    subgraph parentServer.bookingItem.debitorRel.contact:roles[ ]
        style parentServer.bookingItem.debitorRel.contact:roles fill:#99bcdb,stroke:white

        role:parentServer.bookingItem.debitorRel.contact:OWNER[[parentServer.bookingItem.debitorRel.contact:OWNER]]
        role:parentServer.bookingItem.debitorRel.contact:ADMIN[[parentServer.bookingItem.debitorRel.contact:ADMIN]]
        role:parentServer.bookingItem.debitorRel.contact:REFERRER[[parentServer.bookingItem.debitorRel.contact:REFERRER]]
    end
end

subgraph bookingItem.debitor.partnerRel["`**bookingItem.debitor.partnerRel**`"]
    direction TB
    style bookingItem.debitor.partnerRel fill:#99bcdb,stroke:#274d6e,stroke-width:8px

    subgraph bookingItem.debitor.partnerRel:roles[ ]
        style bookingItem.debitor.partnerRel:roles fill:#99bcdb,stroke:white

        role:bookingItem.debitor.partnerRel:OWNER[[bookingItem.debitor.partnerRel:OWNER]]
        role:bookingItem.debitor.partnerRel:ADMIN[[bookingItem.debitor.partnerRel:ADMIN]]
        role:bookingItem.debitor.partnerRel:AGENT[[bookingItem.debitor.partnerRel:AGENT]]
        role:bookingItem.debitor.partnerRel:TENANT[[bookingItem.debitor.partnerRel:TENANT]]
    end
end

subgraph bookingItem.debitor.partnerRel.anchorPerson["`**bookingItem.debitor.partnerRel.anchorPerson**`"]
    direction TB
    style bookingItem.debitor.partnerRel.anchorPerson fill:#99bcdb,stroke:#274d6e,stroke-width:8px

    subgraph bookingItem.debitor.partnerRel.anchorPerson:roles[ ]
        style bookingItem.debitor.partnerRel.anchorPerson:roles fill:#99bcdb,stroke:white

        role:bookingItem.debitor.partnerRel.anchorPerson:OWNER[[bookingItem.debitor.partnerRel.anchorPerson:OWNER]]
        role:bookingItem.debitor.partnerRel.anchorPerson:ADMIN[[bookingItem.debitor.partnerRel.anchorPerson:ADMIN]]
        role:bookingItem.debitor.partnerRel.anchorPerson:REFERRER[[bookingItem.debitor.partnerRel.anchorPerson:REFERRER]]
    end
end

subgraph bookingItem.debitorRel["`**bookingItem.debitorRel**`"]
    direction TB
    style bookingItem.debitorRel fill:#99bcdb,stroke:#274d6e,stroke-width:8px

    subgraph bookingItem.debitorRel:roles[ ]
        style bookingItem.debitorRel:roles fill:#99bcdb,stroke:white

        role:bookingItem.debitorRel:OWNER[[bookingItem.debitorRel:OWNER]]
        role:bookingItem.debitorRel:ADMIN[[bookingItem.debitorRel:ADMIN]]
        role:bookingItem.debitorRel:AGENT[[bookingItem.debitorRel:AGENT]]
        role:bookingItem.debitorRel:TENANT[[bookingItem.debitorRel:TENANT]]
    end
end

subgraph parentServer.bookingItem.debitor.partnerRel.contact["`**parentServer.bookingItem.debitor.partnerRel.contact**`"]
    direction TB
    style parentServer.bookingItem.debitor.partnerRel.contact fill:#99bcdb,stroke:#274d6e,stroke-width:8px

    subgraph parentServer.bookingItem.debitor.partnerRel.contact:roles[ ]
        style parentServer.bookingItem.debitor.partnerRel.contact:roles fill:#99bcdb,stroke:white

        role:parentServer.bookingItem.debitor.partnerRel.contact:OWNER[[parentServer.bookingItem.debitor.partnerRel.contact:OWNER]]
        role:parentServer.bookingItem.debitor.partnerRel.contact:ADMIN[[parentServer.bookingItem.debitor.partnerRel.contact:ADMIN]]
        role:parentServer.bookingItem.debitor.partnerRel.contact:REFERRER[[parentServer.bookingItem.debitor.partnerRel.contact:REFERRER]]
    end
end

subgraph bookingItem.debitorRel.anchorPerson["`**bookingItem.debitorRel.anchorPerson**`"]
    direction TB
    style bookingItem.debitorRel.anchorPerson fill:#99bcdb,stroke:#274d6e,stroke-width:8px

    subgraph bookingItem.debitorRel.anchorPerson:roles[ ]
        style bookingItem.debitorRel.anchorPerson:roles fill:#99bcdb,stroke:white

        role:bookingItem.debitorRel.anchorPerson:OWNER[[bookingItem.debitorRel.anchorPerson:OWNER]]
        role:bookingItem.debitorRel.anchorPerson:ADMIN[[bookingItem.debitorRel.anchorPerson:ADMIN]]
        role:bookingItem.debitorRel.anchorPerson:REFERRER[[bookingItem.debitorRel.anchorPerson:REFERRER]]
    end
end

subgraph parentServer.bookingItem.debitor.debitorRel["`**parentServer.bookingItem.debitor.debitorRel**`"]
    direction TB
    style parentServer.bookingItem.debitor.debitorRel fill:#99bcdb,stroke:#274d6e,stroke-width:8px

    subgraph parentServer.bookingItem.debitor.debitorRel:roles[ ]
        style parentServer.bookingItem.debitor.debitorRel:roles fill:#99bcdb,stroke:white

        role:parentServer.bookingItem.debitor.debitorRel:OWNER[[parentServer.bookingItem.debitor.debitorRel:OWNER]]
        role:parentServer.bookingItem.debitor.debitorRel:ADMIN[[parentServer.bookingItem.debitor.debitorRel:ADMIN]]
        role:parentServer.bookingItem.debitor.debitorRel:AGENT[[parentServer.bookingItem.debitor.debitorRel:AGENT]]
        role:parentServer.bookingItem.debitor.debitorRel:TENANT[[parentServer.bookingItem.debitor.debitorRel:TENANT]]
    end
end

subgraph bookingItem.debitorRel.holderPerson["`**bookingItem.debitorRel.holderPerson**`"]
    direction TB
    style bookingItem.debitorRel.holderPerson fill:#99bcdb,stroke:#274d6e,stroke-width:8px

    subgraph bookingItem.debitorRel.holderPerson:roles[ ]
        style bookingItem.debitorRel.holderPerson:roles fill:#99bcdb,stroke:white

        role:bookingItem.debitorRel.holderPerson:OWNER[[bookingItem.debitorRel.holderPerson:OWNER]]
        role:bookingItem.debitorRel.holderPerson:ADMIN[[bookingItem.debitorRel.holderPerson:ADMIN]]
        role:bookingItem.debitorRel.holderPerson:REFERRER[[bookingItem.debitorRel.holderPerson:REFERRER]]
    end
end

subgraph bookingItem.debitor.refundBankAccount["`**bookingItem.debitor.refundBankAccount**`"]
    direction TB
    style bookingItem.debitor.refundBankAccount fill:#99bcdb,stroke:#274d6e,stroke-width:8px

    subgraph bookingItem.debitor.refundBankAccount:roles[ ]
        style bookingItem.debitor.refundBankAccount:roles fill:#99bcdb,stroke:white

        role:bookingItem.debitor.refundBankAccount:OWNER[[bookingItem.debitor.refundBankAccount:OWNER]]
        role:bookingItem.debitor.refundBankAccount:ADMIN[[bookingItem.debitor.refundBankAccount:ADMIN]]
        role:bookingItem.debitor.refundBankAccount:REFERRER[[bookingItem.debitor.refundBankAccount:REFERRER]]
    end
end

subgraph parentServer.bookingItem.debitor.partnerRel["`**parentServer.bookingItem.debitor.partnerRel**`"]
    direction TB
    style parentServer.bookingItem.debitor.partnerRel fill:#99bcdb,stroke:#274d6e,stroke-width:8px

    subgraph parentServer.bookingItem.debitor.partnerRel:roles[ ]
        style parentServer.bookingItem.debitor.partnerRel:roles fill:#99bcdb,stroke:white

        role:parentServer.bookingItem.debitor.partnerRel:OWNER[[parentServer.bookingItem.debitor.partnerRel:OWNER]]
        role:parentServer.bookingItem.debitor.partnerRel:ADMIN[[parentServer.bookingItem.debitor.partnerRel:ADMIN]]
        role:parentServer.bookingItem.debitor.partnerRel:AGENT[[parentServer.bookingItem.debitor.partnerRel:AGENT]]
        role:parentServer.bookingItem.debitor.partnerRel:TENANT[[parentServer.bookingItem.debitor.partnerRel:TENANT]]
    end
end

subgraph bookingItem.debitor.debitorRel.contact["`**bookingItem.debitor.debitorRel.contact**`"]
    direction TB
    style bookingItem.debitor.debitorRel.contact fill:#99bcdb,stroke:#274d6e,stroke-width:8px

    subgraph bookingItem.debitor.debitorRel.contact:roles[ ]
        style bookingItem.debitor.debitorRel.contact:roles fill:#99bcdb,stroke:white

        role:bookingItem.debitor.debitorRel.contact:OWNER[[bookingItem.debitor.debitorRel.contact:OWNER]]
        role:bookingItem.debitor.debitorRel.contact:ADMIN[[bookingItem.debitor.debitorRel.contact:ADMIN]]
        role:bookingItem.debitor.debitorRel.contact:REFERRER[[bookingItem.debitor.debitorRel.contact:REFERRER]]
    end
end

subgraph parentServer.bookingItem.debitor["`**parentServer.bookingItem.debitor**`"]
    direction TB
    style parentServer.bookingItem.debitor fill:#99bcdb,stroke:#274d6e,stroke-width:8px
end

subgraph parentServer.bookingItem.debitor.debitorRel.holderPerson["`**parentServer.bookingItem.debitor.debitorRel.holderPerson**`"]
    direction TB
    style parentServer.bookingItem.debitor.debitorRel.holderPerson fill:#99bcdb,stroke:#274d6e,stroke-width:8px

    subgraph parentServer.bookingItem.debitor.debitorRel.holderPerson:roles[ ]
        style parentServer.bookingItem.debitor.debitorRel.holderPerson:roles fill:#99bcdb,stroke:white

        role:parentServer.bookingItem.debitor.debitorRel.holderPerson:OWNER[[parentServer.bookingItem.debitor.debitorRel.holderPerson:OWNER]]
        role:parentServer.bookingItem.debitor.debitorRel.holderPerson:ADMIN[[parentServer.bookingItem.debitor.debitorRel.holderPerson:ADMIN]]
        role:parentServer.bookingItem.debitor.debitorRel.holderPerson:REFERRER[[parentServer.bookingItem.debitor.debitorRel.holderPerson:REFERRER]]
    end
end

subgraph bookingItem.debitor.partnerRel.contact["`**bookingItem.debitor.partnerRel.contact**`"]
    direction TB
    style bookingItem.debitor.partnerRel.contact fill:#99bcdb,stroke:#274d6e,stroke-width:8px

    subgraph bookingItem.debitor.partnerRel.contact:roles[ ]
        style bookingItem.debitor.partnerRel.contact:roles fill:#99bcdb,stroke:white

        role:bookingItem.debitor.partnerRel.contact:OWNER[[bookingItem.debitor.partnerRel.contact:OWNER]]
        role:bookingItem.debitor.partnerRel.contact:ADMIN[[bookingItem.debitor.partnerRel.contact:ADMIN]]
        role:bookingItem.debitor.partnerRel.contact:REFERRER[[bookingItem.debitor.partnerRel.contact:REFERRER]]
    end
end

subgraph parentServer.bookingItem.debitorRel["`**parentServer.bookingItem.debitorRel**`"]
    direction TB
    style parentServer.bookingItem.debitorRel fill:#99bcdb,stroke:#274d6e,stroke-width:8px

    subgraph parentServer.bookingItem.debitorRel:roles[ ]
        style parentServer.bookingItem.debitorRel:roles fill:#99bcdb,stroke:white

        role:parentServer.bookingItem.debitorRel:OWNER[[parentServer.bookingItem.debitorRel:OWNER]]
        role:parentServer.bookingItem.debitorRel:ADMIN[[parentServer.bookingItem.debitorRel:ADMIN]]
        role:parentServer.bookingItem.debitorRel:AGENT[[parentServer.bookingItem.debitorRel:AGENT]]
        role:parentServer.bookingItem.debitorRel:TENANT[[parentServer.bookingItem.debitorRel:TENANT]]
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

subgraph parentServer.parentServer["`**parentServer.parentServer**`"]
    direction TB
    style parentServer.parentServer fill:#99bcdb,stroke:#274d6e,stroke-width:8px
end

subgraph parentServer.bookingItem.debitor.debitorRel.contact["`**parentServer.bookingItem.debitor.debitorRel.contact**`"]
    direction TB
    style parentServer.bookingItem.debitor.debitorRel.contact fill:#99bcdb,stroke:#274d6e,stroke-width:8px

    subgraph parentServer.bookingItem.debitor.debitorRel.contact:roles[ ]
        style parentServer.bookingItem.debitor.debitorRel.contact:roles fill:#99bcdb,stroke:white

        role:parentServer.bookingItem.debitor.debitorRel.contact:OWNER[[parentServer.bookingItem.debitor.debitorRel.contact:OWNER]]
        role:parentServer.bookingItem.debitor.debitorRel.contact:ADMIN[[parentServer.bookingItem.debitor.debitorRel.contact:ADMIN]]
        role:parentServer.bookingItem.debitor.debitorRel.contact:REFERRER[[parentServer.bookingItem.debitor.debitorRel.contact:REFERRER]]
    end
end

subgraph bookingItem.debitor.partnerRel.holderPerson["`**bookingItem.debitor.partnerRel.holderPerson**`"]
    direction TB
    style bookingItem.debitor.partnerRel.holderPerson fill:#99bcdb,stroke:#274d6e,stroke-width:8px

    subgraph bookingItem.debitor.partnerRel.holderPerson:roles[ ]
        style bookingItem.debitor.partnerRel.holderPerson:roles fill:#99bcdb,stroke:white

        role:bookingItem.debitor.partnerRel.holderPerson:OWNER[[bookingItem.debitor.partnerRel.holderPerson:OWNER]]
        role:bookingItem.debitor.partnerRel.holderPerson:ADMIN[[bookingItem.debitor.partnerRel.holderPerson:ADMIN]]
        role:bookingItem.debitor.partnerRel.holderPerson:REFERRER[[bookingItem.debitor.partnerRel.holderPerson:REFERRER]]
    end
end

subgraph bookingItem.debitorRel.contact["`**bookingItem.debitorRel.contact**`"]
    direction TB
    style bookingItem.debitorRel.contact fill:#99bcdb,stroke:#274d6e,stroke-width:8px

    subgraph bookingItem.debitorRel.contact:roles[ ]
        style bookingItem.debitorRel.contact:roles fill:#99bcdb,stroke:white

        role:bookingItem.debitorRel.contact:OWNER[[bookingItem.debitorRel.contact:OWNER]]
        role:bookingItem.debitorRel.contact:ADMIN[[bookingItem.debitorRel.contact:ADMIN]]
        role:bookingItem.debitorRel.contact:REFERRER[[bookingItem.debitorRel.contact:REFERRER]]
    end
end

subgraph parentServer.bookingItem.debitor.refundBankAccount["`**parentServer.bookingItem.debitor.refundBankAccount**`"]
    direction TB
    style parentServer.bookingItem.debitor.refundBankAccount fill:#99bcdb,stroke:#274d6e,stroke-width:8px

    subgraph parentServer.bookingItem.debitor.refundBankAccount:roles[ ]
        style parentServer.bookingItem.debitor.refundBankAccount:roles fill:#99bcdb,stroke:white

        role:parentServer.bookingItem.debitor.refundBankAccount:OWNER[[parentServer.bookingItem.debitor.refundBankAccount:OWNER]]
        role:parentServer.bookingItem.debitor.refundBankAccount:ADMIN[[parentServer.bookingItem.debitor.refundBankAccount:ADMIN]]
        role:parentServer.bookingItem.debitor.refundBankAccount:REFERRER[[parentServer.bookingItem.debitor.refundBankAccount:REFERRER]]
    end
end

subgraph bookingItem.debitor["`**bookingItem.debitor**`"]
    direction TB
    style bookingItem.debitor fill:#99bcdb,stroke:#274d6e,stroke-width:8px
end

subgraph bookingItem.debitor.debitorRel.holderPerson["`**bookingItem.debitor.debitorRel.holderPerson**`"]
    direction TB
    style bookingItem.debitor.debitorRel.holderPerson fill:#99bcdb,stroke:#274d6e,stroke-width:8px

    subgraph bookingItem.debitor.debitorRel.holderPerson:roles[ ]
        style bookingItem.debitor.debitorRel.holderPerson:roles fill:#99bcdb,stroke:white

        role:bookingItem.debitor.debitorRel.holderPerson:OWNER[[bookingItem.debitor.debitorRel.holderPerson:OWNER]]
        role:bookingItem.debitor.debitorRel.holderPerson:ADMIN[[bookingItem.debitor.debitorRel.holderPerson:ADMIN]]
        role:bookingItem.debitor.debitorRel.holderPerson:REFERRER[[bookingItem.debitor.debitorRel.holderPerson:REFERRER]]
    end
end

subgraph bookingItem.debitor.debitorRel["`**bookingItem.debitor.debitorRel**`"]
    direction TB
    style bookingItem.debitor.debitorRel fill:#99bcdb,stroke:#274d6e,stroke-width:8px

    subgraph bookingItem.debitor.debitorRel:roles[ ]
        style bookingItem.debitor.debitorRel:roles fill:#99bcdb,stroke:white

        role:bookingItem.debitor.debitorRel:OWNER[[bookingItem.debitor.debitorRel:OWNER]]
        role:bookingItem.debitor.debitorRel:ADMIN[[bookingItem.debitor.debitorRel:ADMIN]]
        role:bookingItem.debitor.debitorRel:AGENT[[bookingItem.debitor.debitorRel:AGENT]]
        role:bookingItem.debitor.debitorRel:TENANT[[bookingItem.debitor.debitorRel:TENANT]]
    end
end

subgraph asset["`**asset**`"]
    direction TB
    style asset fill:#dd4901,stroke:#274d6e,stroke-width:8px

    subgraph asset:roles[ ]
        style asset:roles fill:#dd4901,stroke:white

        role:asset:OWNER[[asset:OWNER]]
        role:asset:ADMIN[[asset:ADMIN]]
        role:asset:TENANT[[asset:TENANT]]
    end

    subgraph asset:permissions[ ]
        style asset:permissions fill:#dd4901,stroke:white

        perm:asset:INSERT{{asset:INSERT}}
        perm:asset:DELETE{{asset:DELETE}}
        perm:asset:UPDATE{{asset:UPDATE}}
        perm:asset:SELECT{{asset:SELECT}}
    end
end

subgraph parentServer.bookingItem.debitor.debitorRel.anchorPerson["`**parentServer.bookingItem.debitor.debitorRel.anchorPerson**`"]
    direction TB
    style parentServer.bookingItem.debitor.debitorRel.anchorPerson fill:#99bcdb,stroke:#274d6e,stroke-width:8px

    subgraph parentServer.bookingItem.debitor.debitorRel.anchorPerson:roles[ ]
        style parentServer.bookingItem.debitor.debitorRel.anchorPerson:roles fill:#99bcdb,stroke:white

        role:parentServer.bookingItem.debitor.debitorRel.anchorPerson:OWNER[[parentServer.bookingItem.debitor.debitorRel.anchorPerson:OWNER]]
        role:parentServer.bookingItem.debitor.debitorRel.anchorPerson:ADMIN[[parentServer.bookingItem.debitor.debitorRel.anchorPerson:ADMIN]]
        role:parentServer.bookingItem.debitor.debitorRel.anchorPerson:REFERRER[[parentServer.bookingItem.debitor.debitorRel.anchorPerson:REFERRER]]
    end
end

%% granting roles to roles
role:global:ADMIN -.-> role:bookingItem.debitor.refundBankAccount:OWNER
role:bookingItem.debitor.refundBankAccount:OWNER -.-> role:bookingItem.debitor.refundBankAccount:ADMIN
role:bookingItem.debitor.refundBankAccount:ADMIN -.-> role:bookingItem.debitor.refundBankAccount:REFERRER
role:bookingItem.debitor.refundBankAccount:ADMIN -.-> role:bookingItem.debitor.debitorRel:AGENT
role:bookingItem.debitor.debitorRel:AGENT -.-> role:bookingItem.debitor.refundBankAccount:REFERRER
role:global:ADMIN -.-> role:bookingItem.debitor.partnerRel:OWNER
role:bookingItem.debitor.partnerRel:OWNER -.-> role:bookingItem.debitor.partnerRel:ADMIN
role:bookingItem.debitor.partnerRel:ADMIN -.-> role:bookingItem.debitor.partnerRel:AGENT
role:bookingItem.debitor.partnerRel:AGENT -.-> role:bookingItem.debitor.partnerRel:TENANT
role:bookingItem.debitor.partnerRel:ADMIN -.-> role:bookingItem.debitor.debitorRel:ADMIN
role:bookingItem.debitor.partnerRel:AGENT -.-> role:bookingItem.debitor.debitorRel:AGENT
role:bookingItem.debitor.debitorRel:AGENT -.-> role:bookingItem.debitor.partnerRel:TENANT
role:global:ADMIN -.-> role:bookingItem.debitorRel.anchorPerson:OWNER
role:bookingItem.debitorRel.anchorPerson:OWNER -.-> role:bookingItem.debitorRel.anchorPerson:ADMIN
role:bookingItem.debitorRel.anchorPerson:ADMIN -.-> role:bookingItem.debitorRel.anchorPerson:REFERRER
role:global:ADMIN -.-> role:bookingItem.debitorRel.holderPerson:OWNER
role:bookingItem.debitorRel.holderPerson:OWNER -.-> role:bookingItem.debitorRel.holderPerson:ADMIN
role:bookingItem.debitorRel.holderPerson:ADMIN -.-> role:bookingItem.debitorRel.holderPerson:REFERRER
role:global:ADMIN -.-> role:bookingItem.debitorRel.contact:OWNER
role:bookingItem.debitorRel.contact:OWNER -.-> role:bookingItem.debitorRel.contact:ADMIN
role:bookingItem.debitorRel.contact:ADMIN -.-> role:bookingItem.debitorRel.contact:REFERRER
role:bookingItem.debitorRel:AGENT -.-> role:bookingItem:OWNER
role:bookingItem:OWNER -.-> role:bookingItem:ADMIN
role:bookingItem.debitorRel:AGENT -.-> role:bookingItem:ADMIN
role:bookingItem:ADMIN -.-> role:bookingItem:AGENT
role:bookingItem:AGENT -.-> role:bookingItem:TENANT
role:bookingItem:TENANT -.-> role:bookingItem.debitorRel:TENANT
role:bookingItem:ADMIN ==> role:asset:OWNER
role:asset:OWNER ==> role:asset:ADMIN
role:asset:ADMIN ==> role:asset:TENANT
role:asset:TENANT ==> role:bookingItem:TENANT

%% granting permissions to roles
role:bookingItem:AGENT ==> perm:asset:INSERT
role:asset:OWNER ==> perm:asset:DELETE
role:asset:ADMIN ==> perm:asset:UPDATE
role:asset:TENANT ==> perm:asset:SELECT

```
