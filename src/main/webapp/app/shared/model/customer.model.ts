import { Moment } from 'moment';
import { IMembership } from 'app/shared/model/membership.model';
import { ISepaMandate } from 'app/shared/model/sepa-mandate.model';

export const enum CustomerKind {
    NATURAL = 'NATURAL',
    LEGAL = 'LEGAL'
}

export const enum VatRegion {
    DOMESTIC = 'DOMESTIC',
    EU = 'EU',
    OTHER = 'OTHER'
}

export interface ICustomer {
    id?: number;
    reference?: number;
    prefix?: string;
    name?: string;
    kind?: CustomerKind;
    birthDate?: Moment;
    birthPlace?: string;
    registrationCourt?: string;
    registrationNumber?: string;
    vatRegion?: VatRegion;
    vatNumber?: string;
    contractualSalutation?: string;
    contractualAddress?: string;
    billingSalutation?: string;
    billingAddress?: string;
    remark?: string;
    memberships?: IMembership[];
    sepamandates?: ISepaMandate[];
    displayLabel?: string;
}

export class Customer implements ICustomer {
    constructor(
        public id?: number,
        public reference?: number,
        public prefix?: string,
        public name?: string,
        public kind?: CustomerKind,
        public birthDate?: Moment,
        public birthPlace?: string,
        public registrationCourt?: string,
        public registrationNumber?: string,
        public vatRegion?: VatRegion,
        public vatNumber?: string,
        public contractualSalutation?: string,
        public contractualAddress?: string,
        public billingSalutation?: string,
        public billingAddress?: string,
        public remark?: string,
        public memberships?: IMembership[],
        public sepamandates?: ISepaMandate[],
        public displayLabel?: string
    ) {}
}
