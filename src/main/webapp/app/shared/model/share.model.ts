import { Moment } from 'moment';

export const enum ShareAction {
    SUBSCRIPTION = 'SUBSCRIPTION',
    CANCELLATION = 'CANCELLATION'
}

export interface IShare {
    id?: number;
    documentDate?: Moment;
    valueDate?: Moment;
    action?: ShareAction;
    quantity?: number;
    remark?: string;
    membershipDisplayLabel?: string;
    membershipId?: number;
}

export class Share implements IShare {
    constructor(
        public id?: number,
        public documentDate?: Moment,
        public valueDate?: Moment,
        public action?: ShareAction,
        public quantity?: number,
        public remark?: string,
        public membershipDisplayLabel?: string,
        public membershipId?: number
    ) {}
}
