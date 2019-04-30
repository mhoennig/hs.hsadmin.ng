import { IUser } from 'app/core/user/user.model';

export const enum UserRole {
    HOSTMASTER = 'HOSTMASTER',
    ADMIN = 'ADMIN',
    SUPPORTER = 'SUPPORTER',
    CONTRACTUAL_CONTACT = 'CONTRACTUAL_CONTACT',
    FINANCIAL_CONTACT = 'FINANCIAL_CONTACT',
    TECHNICAL_CONTACT = 'TECHNICAL_CONTACT',
    CUSTOMER_USER = 'CUSTOMER_USER'
}

export interface IUserRoleAssignment {
    id?: number;
    entityTypeId?: string;
    entityObjectId?: number;
    userId?: number;
    assignedRole?: UserRole;
    user?: IUser;
}

export class UserRoleAssignment implements IUserRoleAssignment {
    constructor(
        public id?: number,
        public entityTypeId?: string,
        public entityObjectId?: number,
        public userId?: number,
        public assignedRole?: UserRole,
        public user?: IUser
    ) {}
}
