import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import * as moment from 'moment';
import { DATE_FORMAT } from 'app/shared/constants/input.constants';
import { map } from 'rxjs/operators';

import { SERVER_API_URL } from 'app/app.constants';
import { createRequestOption } from 'app/shared';
import { IShare } from 'app/shared/model/share.model';

type EntityResponseType = HttpResponse<IShare>;
type EntityArrayResponseType = HttpResponse<IShare[]>;

@Injectable({ providedIn: 'root' })
export class ShareService {
    public resourceUrl = SERVER_API_URL + 'api/shares';

    constructor(protected http: HttpClient) {}

    create(share: IShare): Observable<EntityResponseType> {
        const copy = this.convertDateFromClient(share);
        return this.http
            .post<IShare>(this.resourceUrl, copy, { observe: 'response' })
            .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
    }

    update(share: IShare): Observable<EntityResponseType> {
        const copy = this.convertDateFromClient(share);
        return this.http
            .put<IShare>(this.resourceUrl, copy, { observe: 'response' })
            .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
    }

    find(id: number): Observable<EntityResponseType> {
        return this.http
            .get<IShare>(`${this.resourceUrl}/${id}`, { observe: 'response' })
            .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
    }

    query(req?: any): Observable<EntityArrayResponseType> {
        const options = createRequestOption(req);
        return this.http
            .get<IShare[]>(this.resourceUrl, { params: options, observe: 'response' })
            .pipe(map((res: EntityArrayResponseType) => this.convertDateArrayFromServer(res)));
    }

    delete(id: number): Observable<HttpResponse<any>> {
        return this.http.delete<any>(`${this.resourceUrl}/${id}`, { observe: 'response' });
    }

    protected convertDateFromClient(share: IShare): IShare {
        const copy: IShare = Object.assign({}, share, {
            documentDate: share.documentDate != null && share.documentDate.isValid() ? share.documentDate.format(DATE_FORMAT) : null,
            valueDate: share.valueDate != null && share.valueDate.isValid() ? share.valueDate.format(DATE_FORMAT) : null
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
            res.body.forEach((share: IShare) => {
                share.documentDate = share.documentDate != null ? moment(share.documentDate) : null;
                share.valueDate = share.valueDate != null ? moment(share.valueDate) : null;
            });
        }
        return res;
    }
}
