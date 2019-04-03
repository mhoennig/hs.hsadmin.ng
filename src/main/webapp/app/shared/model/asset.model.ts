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
    date?: Moment;
    action?: AssetAction;
    amount?: number;
    comment?: string;
    memberId?: number;
}

export class Asset implements IAsset {
    constructor(
        public id?: number,
        public date?: Moment,
        public action?: AssetAction,
        public amount?: number,
        public comment?: string,
        public memberId?: number
    ) {}
}
