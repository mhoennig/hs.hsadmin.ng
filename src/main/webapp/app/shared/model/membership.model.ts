import { Moment } from 'moment';
import { IShare } from 'app/shared/model/share.model';
import { IAsset } from 'app/shared/model/asset.model';

export interface IMembership {
    id?: number;
    admissionDocumentDate?: Moment;
    cancellationDocumentDate?: Moment;
    memberFromDate?: Moment;
    memberUntilDate?: Moment;
    remark?: string;
    shares?: IShare[];
    assets?: IAsset[];
    customerPrefix?: string;
    customerId?: number;
    customerDisplayLabel?: string;
    displayLabel?: string;
}

export class Membership implements IMembership {
    constructor(
        public id?: number,
        public admissionDocumentDate?: Moment,
        public cancellationDocumentDate?: Moment,
        public memberFromDate?: Moment,
        public memberUntilDate?: Moment,
        public remark?: string,
        public shares?: IShare[],
        public assets?: IAsset[],
        public customerPrefix?: string,
        public customerId?: number,
        public customerDisplayLabel?: string,
        public displayLabel?: string
    ) {}
}
