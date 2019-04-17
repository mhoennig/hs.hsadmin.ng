import { Moment } from 'moment';

export interface ISepaMandate {
    id?: number;
    reference?: string;
    iban?: string;
    bic?: string;
    created?: Moment;
    validFrom?: Moment;
    validTo?: Moment;
    lastUsed?: Moment;
    cancelled?: Moment;
    comment?: string;
    customerPrefix?: string;
    customerId?: number;
}

export class SepaMandate implements ISepaMandate {
    constructor(
        public id?: number,
        public reference?: string,
        public iban?: string,
        public bic?: string,
        public created?: Moment,
        public validFrom?: Moment,
        public validTo?: Moment,
        public lastUsed?: Moment,
        public cancelled?: Moment,
        public comment?: string,
        public customerPrefix?: string,
        public customerId?: number
    ) {}
}
