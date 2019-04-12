import { ICustomerContact } from 'app/shared/model/customer-contact.model';
import { IMembership } from 'app/shared/model/membership.model';

export interface ICustomer {
    id?: number;
    number?: number;
    prefix?: string;
    name?: string;
    contractualAddress?: string;
    contractualSalutation?: string;
    billingAddress?: string;
    billingSalutation?: string;
    roles?: ICustomerContact[];
    memberships?: IMembership[];
}

export class Customer implements ICustomer {
    constructor(
        public id?: number,
        public number?: number,
        public prefix?: string,
        public name?: string,
        public contractualAddress?: string,
        public contractualSalutation?: string,
        public billingAddress?: string,
        public billingSalutation?: string,
        public roles?: ICustomerContact[],
        public memberships?: IMembership[]
    ) {}
}
