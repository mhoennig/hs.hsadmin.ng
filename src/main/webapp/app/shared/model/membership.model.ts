import { Moment } from 'moment';
import { IShare } from 'app/shared/model/share.model';
import { IAsset } from 'app/shared/model/asset.model';

export interface IMembership {
    id?: number;
    from?: Moment;
    to?: Moment;
    comment?: string;
    shares?: IShare[];
    assets?: IAsset[];
    customerPrefix?: string;
    customerId?: number;
}

export class Membership implements IMembership {
    constructor(
        public id?: number,
        public from?: Moment,
        public to?: Moment,
        public comment?: string,
        public shares?: IShare[],
        public assets?: IAsset[],
        public customerPrefix?: string,
        public customerId?: number
    ) {}
}
