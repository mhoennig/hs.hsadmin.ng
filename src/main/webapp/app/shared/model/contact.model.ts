import { ICustomerContact } from 'app/shared/model/customer-contact.model';

export interface IContact {
    id?: number;
    firstName?: string;
    lastName?: string;
    email?: string;
    roles?: ICustomerContact[];
}

export class Contact implements IContact {
    constructor(
        public id?: number,
        public firstName?: string,
        public lastName?: string,
        public email?: string,
        public roles?: ICustomerContact[]
    ) {}
}
