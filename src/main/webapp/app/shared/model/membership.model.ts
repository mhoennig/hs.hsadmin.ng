import { Moment } from 'moment';
import { IShare } from 'app/shared/model/share.model';
import { IAsset } from 'app/shared/model/asset.model';

export interface IMembership {
    id?: number;
    sinceDate?: Moment;
    untilDate?: Moment;
    shares?: IShare[];
    assets?: IAsset[];
    customerPrefix?: string;
    customerId?: number;
}

export class Membership implements IMembership {
    constructor(
        public id?: number,
        public sinceDate?: Moment,
        public untilDate?: Moment,
        public shares?: IShare[],
        public assets?: IAsset[],
        public customerPrefix?: string,
        public customerId?: number
    ) {}
}
