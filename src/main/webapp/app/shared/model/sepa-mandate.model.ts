import { Moment } from 'moment';

export interface ISepaMandate {
    id?: number;
    reference?: string;
    iban?: string;
    bic?: string;
    documentDate?: Moment;
    validFrom?: Moment;
    validUntil?: Moment;
    lastUsed?: Moment;
    cancellationDate?: Moment;
    remark?: string;
    customerPrefix?: string;
    customerId?: number;
}

export class SepaMandate implements ISepaMandate {
    constructor(
        public id?: number,
        public reference?: string,
        public iban?: string,
        public bic?: string,
        public documentDate?: Moment,
        public validFrom?: Moment,
        public validUntil?: Moment,
        public lastUsed?: Moment,
        public cancellationDate?: Moment,
        public remark?: string,
        public customerPrefix?: string,
        public customerId?: number
    ) {}
}
