export const enum CustomerContactRole {
    CONTRACTUAL = 'CONTRACTUAL',
    TECHNICAL = 'TECHNICAL',
    FINANCIAL = 'FINANCIAL'
}

export interface ICustomerContact {
    id?: number;
    role?: CustomerContactRole;
    contactEmail?: string;
    contactId?: number;
    customerPrefix?: string;
    customerId?: number;
}

export class CustomerContact implements ICustomerContact {
    constructor(
        public id?: number,
        public role?: CustomerContactRole,
        public contactEmail?: string,
        public contactId?: number,
        public customerPrefix?: string,
        public customerId?: number
    ) {}
}
