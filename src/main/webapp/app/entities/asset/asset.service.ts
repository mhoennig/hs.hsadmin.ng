import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import * as moment from 'moment';
import { DATE_FORMAT } from 'app/shared/constants/input.constants';
import { map } from 'rxjs/operators';

import { SERVER_API_URL } from 'app/app.constants';
import { createRequestOption } from 'app/shared';
import { IAsset } from 'app/shared/model/asset.model';

type EntityResponseType = HttpResponse<IAsset>;
type EntityArrayResponseType = HttpResponse<IAsset[]>;

@Injectable({ providedIn: 'root' })
export class AssetService {
    public resourceUrl = SERVER_API_URL + 'api/assets';

    constructor(protected http: HttpClient) {}

    create(asset: IAsset): Observable<EntityResponseType> {
        const copy = this.convertDateFromClient(asset);
        return this.http
            .post<IAsset>(this.resourceUrl, copy, { observe: 'response' })
            .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
    }

    update(asset: IAsset): Observable<EntityResponseType> {
        const copy = this.convertDateFromClient(asset);
        return this.http
            .put<IAsset>(this.resourceUrl, copy, { observe: 'response' })
            .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
    }

    find(id: number): Observable<EntityResponseType> {
        return this.http
            .get<IAsset>(`${this.resourceUrl}/${id}`, { observe: 'response' })
            .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
    }

    query(req?: any): Observable<EntityArrayResponseType> {
        const options = createRequestOption(req);
        return this.http
            .get<IAsset[]>(this.resourceUrl, { params: options, observe: 'response' })
            .pipe(map((res: EntityArrayResponseType) => this.convertDateArrayFromServer(res)));
    }

    delete(id: number): Observable<HttpResponse<any>> {
        return this.http.delete<any>(`${this.resourceUrl}/${id}`, { observe: 'response' });
    }

    protected convertDateFromClient(asset: IAsset): IAsset {
        const copy: IAsset = Object.assign({}, asset, {
            documentDate: asset.documentDate != null && asset.documentDate.isValid() ? asset.documentDate.format(DATE_FORMAT) : null,
            valueDate: asset.valueDate != null && asset.valueDate.isValid() ? asset.valueDate.format(DATE_FORMAT) : null
        });
        return copy;
    }

    protected convertDateFromServer(res: EntityResponseType): EntityResponseType {
        if (res.body) {
            res.body.documentDate = res.body.documentDate != null ? moment(res.body.documentDate) : null;
            res.body.valueDate = res.body.valueDate != null ? moment(res.body.valueDate) : null;
        }
        return res;
    }

    protected convertDateArrayFromServer(res: EntityArrayResponseType): EntityArrayResponseType {
        if (res.body) {
            res.body.forEach((asset: IAsset) => {
                asset.documentDate = asset.documentDate != null ? moment(asset.documentDate) : null;
                asset.valueDate = asset.valueDate != null ? moment(asset.valueDate) : null;
            });
        }
        return res;
    }
}
