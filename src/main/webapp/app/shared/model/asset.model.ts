import { Moment } from 'moment';

export const enum AssetAction {
    PAYMENT = 'PAYMENT',
    HANDOVER = 'HANDOVER',
    ADOPTION = 'ADOPTION',
    LOSS = 'LOSS',
    CLEARING = 'CLEARING',
    PAYBACK = 'PAYBACK'
}

export interface IAsset {
    id?: number;
    documentDate?: Moment;
    valueDate?: Moment;
    action?: AssetAction;
    amount?: number;
    remark?: string;
    membershipDisplayReference?: string;
    membershipId?: number;
}

export class Asset implements IAsset {
    constructor(
        public id?: number,
        public documentDate?: Moment,
        public valueDate?: Moment,
        public action?: AssetAction,
        public amount?: number,
        public remark?: string,
        public membershipDisplayReference?: string,
        public membershipId?: number
    ) {}
}
