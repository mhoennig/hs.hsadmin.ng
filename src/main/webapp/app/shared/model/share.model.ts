import { Moment } from 'moment';

export const enum ShareAction {
    SUBSCRIPTION = 'SUBSCRIPTION',
    CANCELLATION = 'CANCELLATION'
}

export interface IShare {
    id?: number;
    date?: Moment;
    action?: ShareAction;
    quantity?: number;
    comment?: string;
    memberId?: number;
}

export class Share implements IShare {
    constructor(
        public id?: number,
        public date?: Moment,
        public action?: ShareAction,
        public quantity?: number,
        public comment?: string,
        public memberId?: number
    ) {}
}
