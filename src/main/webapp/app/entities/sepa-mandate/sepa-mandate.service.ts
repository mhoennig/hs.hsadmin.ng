import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import * as moment from 'moment';
import { DATE_FORMAT } from 'app/shared/constants/input.constants';
import { map } from 'rxjs/operators';

import { SERVER_API_URL } from 'app/app.constants';
import { createRequestOption } from 'app/shared';
import { ISepaMandate } from 'app/shared/model/sepa-mandate.model';

type EntityResponseType = HttpResponse<ISepaMandate>;
type EntityArrayResponseType = HttpResponse<ISepaMandate[]>;

@Injectable({ providedIn: 'root' })
export class SepaMandateService {
    public resourceUrl = SERVER_API_URL + 'api/sepa-mandates';

    constructor(protected http: HttpClient) {}

    create(sepaMandate: ISepaMandate): Observable<EntityResponseType> {
        const copy = this.convertDateFromClient(sepaMandate);
        return this.http
            .post<ISepaMandate>(this.resourceUrl, copy, { observe: 'response' })
            .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
    }

    update(sepaMandate: ISepaMandate): Observable<EntityResponseType> {
        const copy = this.convertDateFromClient(sepaMandate);
        return this.http
            .put<ISepaMandate>(this.resourceUrl, copy, { observe: 'response' })
            .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
    }

    find(id: number): Observable<EntityResponseType> {
        return this.http
            .get<ISepaMandate>(`${this.resourceUrl}/${id}`, { observe: 'response' })
            .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
    }

    query(req?: any): Observable<EntityArrayResponseType> {
        const options = createRequestOption(req);
        return this.http
            .get<ISepaMandate[]>(this.resourceUrl, { params: options, observe: 'response' })
            .pipe(map((res: EntityArrayResponseType) => this.convertDateArrayFromServer(res)));
    }

    delete(id: number): Observable<HttpResponse<any>> {
        return this.http.delete<any>(`${this.resourceUrl}/${id}`, { observe: 'response' });
    }

    protected convertDateFromClient(sepaMandate: ISepaMandate): ISepaMandate {
        const copy: ISepaMandate = Object.assign({}, sepaMandate, {
            created: sepaMandate.created != null && sepaMandate.created.isValid() ? sepaMandate.created.format(DATE_FORMAT) : null,
            validFrom: sepaMandate.validFrom != null && sepaMandate.validFrom.isValid() ? sepaMandate.validFrom.format(DATE_FORMAT) : null,
            validTo: sepaMandate.validTo != null && sepaMandate.validTo.isValid() ? sepaMandate.validTo.format(DATE_FORMAT) : null,
            lastUsed: sepaMandate.lastUsed != null && sepaMandate.lastUsed.isValid() ? sepaMandate.lastUsed.format(DATE_FORMAT) : null,
            cancelled: sepaMandate.cancelled != null && sepaMandate.cancelled.isValid() ? sepaMandate.cancelled.format(DATE_FORMAT) : null
        });
        return copy;
    }

    protected convertDateFromServer(res: EntityResponseType): EntityResponseType {
        if (res.body) {
            res.body.created = res.body.created != null ? moment(res.body.created) : null;
            res.body.validFrom = res.body.validFrom != null ? moment(res.body.validFrom) : null;
            res.body.validTo = res.body.validTo != null ? moment(res.body.validTo) : null;
            res.body.lastUsed = res.body.lastUsed != null ? moment(res.body.lastUsed) : null;
            res.body.cancelled = res.body.cancelled != null ? moment(res.body.cancelled) : null;
        }
        return res;
    }

    protected convertDateArrayFromServer(res: EntityArrayResponseType): EntityArrayResponseType {
        if (res.body) {
            res.body.forEach((sepaMandate: ISepaMandate) => {
                sepaMandate.created = sepaMandate.created != null ? moment(sepaMandate.created) : null;
                sepaMandate.validFrom = sepaMandate.validFrom != null ? moment(sepaMandate.validFrom) : null;
                sepaMandate.validTo = sepaMandate.validTo != null ? moment(sepaMandate.validTo) : null;
                sepaMandate.lastUsed = sepaMandate.lastUsed != null ? moment(sepaMandate.lastUsed) : null;
                sepaMandate.cancelled = sepaMandate.cancelled != null ? moment(sepaMandate.cancelled) : null;
            });
        }
        return res;
    }
}
