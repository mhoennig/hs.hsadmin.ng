import { Moment } from 'moment';

export interface ISepaMandate {
    id?: number;
    reference?: string;
    iban?: string;
    bic?: string;
    grantingDocumentDate?: Moment;
    revokationDocumentDate?: Moment;
    validFromDate?: Moment;
    validUntilDate?: Moment;
    lastUsedDate?: Moment;
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
        public grantingDocumentDate?: Moment,
        public revokationDocumentDate?: Moment,
        public validFromDate?: Moment,
        public validUntilDate?: Moment,
        public lastUsedDate?: Moment,
        public remark?: string,
        public customerPrefix?: string,
        public customerId?: number
    ) {}
}
