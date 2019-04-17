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
            documentDate:
                sepaMandate.documentDate != null && sepaMandate.documentDate.isValid()
                    ? sepaMandate.documentDate.format(DATE_FORMAT)
                    : null,
            validFrom: sepaMandate.validFrom != null && sepaMandate.validFrom.isValid() ? sepaMandate.validFrom.format(DATE_FORMAT) : null,
            validUntil:
                sepaMandate.validUntil != null && sepaMandate.validUntil.isValid() ? sepaMandate.validUntil.format(DATE_FORMAT) : null,
            lastUsed: sepaMandate.lastUsed != null && sepaMandate.lastUsed.isValid() ? sepaMandate.lastUsed.format(DATE_FORMAT) : null,
            cancellationDate:
                sepaMandate.cancellationDate != null && sepaMandate.cancellationDate.isValid()
                    ? sepaMandate.cancellationDate.format(DATE_FORMAT)
                    : null
        });
        return copy;
    }

    protected convertDateFromServer(res: EntityResponseType): EntityResponseType {
        if (res.body) {
            res.body.documentDate = res.body.documentDate != null ? moment(res.body.documentDate) : null;
            res.body.validFrom = res.body.validFrom != null ? moment(res.body.validFrom) : null;
            res.body.validUntil = res.body.validUntil != null ? moment(res.body.validUntil) : null;
            res.body.lastUsed = res.body.lastUsed != null ? moment(res.body.lastUsed) : null;
            res.body.cancellationDate = res.body.cancellationDate != null ? moment(res.body.cancellationDate) : null;
        }
        return res;
    }

    protected convertDateArrayFromServer(res: EntityArrayResponseType): EntityArrayResponseType {
        if (res.body) {
            res.body.forEach((sepaMandate: ISepaMandate) => {
                sepaMandate.documentDate = sepaMandate.documentDate != null ? moment(sepaMandate.documentDate) : null;
                sepaMandate.validFrom = sepaMandate.validFrom != null ? moment(sepaMandate.validFrom) : null;
                sepaMandate.validUntil = sepaMandate.validUntil != null ? moment(sepaMandate.validUntil) : null;
                sepaMandate.lastUsed = sepaMandate.lastUsed != null ? moment(sepaMandate.lastUsed) : null;
                sepaMandate.cancellationDate = sepaMandate.cancellationDate != null ? moment(sepaMandate.cancellationDate) : null;
            });
        }
        return res;
    }
}
