import { Moment } from 'moment';
import { IShare } from 'app/shared/model/share.model';
import { IAsset } from 'app/shared/model/asset.model';

export interface IMembership {
    id?: number;
    documentDate?: Moment;
    memberFrom?: Moment;
    memberUntil?: Moment;
    remark?: string;
    shares?: IShare[];
    assets?: IAsset[];
    customerPrefix?: string;
    customerId?: number;
}

export class Membership implements IMembership {
    constructor(
        public id?: number,
        public documentDate?: Moment,
        public memberFrom?: Moment,
        public memberUntil?: Moment,
        public remark?: string,
        public shares?: IShare[],
        public assets?: IAsset[],
        public customerPrefix?: string,
        public customerId?: number
    ) {}
}
