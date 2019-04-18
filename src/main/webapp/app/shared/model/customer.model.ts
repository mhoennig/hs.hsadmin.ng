import { IMembership } from 'app/shared/model/membership.model';
import { ISepaMandate } from 'app/shared/model/sepa-mandate.model';

export interface ICustomer {
    id?: number;
    reference?: number;
    prefix?: string;
    name?: string;
    contractualSalutation?: string;
    contractualAddress?: string;
    billingSalutation?: string;
    billingAddress?: string;
    remark?: string;
    memberships?: IMembership[];
    sepamandates?: ISepaMandate[];
}

export class Customer implements ICustomer {
    constructor(
        public id?: number,
        public reference?: number,
        public prefix?: string,
        public name?: string,
        public contractualSalutation?: string,
        public contractualAddress?: string,
        public billingSalutation?: string,
        public billingAddress?: string,
        public remark?: string,
        public memberships?: IMembership[],
        public sepamandates?: ISepaMandate[]
    ) {}
}
