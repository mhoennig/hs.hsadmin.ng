import { IMembership } from 'app/shared/model/membership.model';
import { ICustomerContact } from 'app/shared/model/customer-contact.model';

export interface ICustomer {
    id?: number;
    number?: number;
    prefix?: string;
    memberships?: IMembership[];
    roles?: ICustomerContact[];
}

export class Customer implements ICustomer {
    constructor(
        public id?: number,
        public number?: number,
        public prefix?: string,
        public memberships?: IMembership[],
        public roles?: ICustomerContact[]
    ) {}
}
