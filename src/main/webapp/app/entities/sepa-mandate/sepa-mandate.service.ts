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
            grantingDocumentDate:
                sepaMandate.grantingDocumentDate != null && sepaMandate.grantingDocumentDate.isValid()
                    ? sepaMandate.grantingDocumentDate.format(DATE_FORMAT)
                    : null,
            revokationDocumentDate:
                sepaMandate.revokationDocumentDate != null && sepaMandate.revokationDocumentDate.isValid()
                    ? sepaMandate.revokationDocumentDate.format(DATE_FORMAT)
                    : null,
            validFromDate:
                sepaMandate.validFromDate != null && sepaMandate.validFromDate.isValid()
                    ? sepaMandate.validFromDate.format(DATE_FORMAT)
                    : null,
            validUntilDate:
                sepaMandate.validUntilDate != null && sepaMandate.validUntilDate.isValid()
                    ? sepaMandate.validUntilDate.format(DATE_FORMAT)
                    : null,
            lastUsedDate:
                sepaMandate.lastUsedDate != null && sepaMandate.lastUsedDate.isValid() ? sepaMandate.lastUsedDate.format(DATE_FORMAT) : null
        });
        return copy;
    }

    protected convertDateFromServer(res: EntityResponseType): EntityResponseType {
        if (res.body) {
            res.body.grantingDocumentDate = res.body.grantingDocumentDate != null ? moment(res.body.grantingDocumentDate) : null;
            res.body.revokationDocumentDate = res.body.revokationDocumentDate != null ? moment(res.body.revokationDocumentDate) : null;
            res.body.validFromDate = res.body.validFromDate != null ? moment(res.body.validFromDate) : null;
            res.body.validUntilDate = res.body.validUntilDate != null ? moment(res.body.validUntilDate) : null;
            res.body.lastUsedDate = res.body.lastUsedDate != null ? moment(res.body.lastUsedDate) : null;
        }
        return res;
    }

    protected convertDateArrayFromServer(res: EntityArrayResponseType): EntityArrayResponseType {
        if (res.body) {
            res.body.forEach((sepaMandate: ISepaMandate) => {
                sepaMandate.grantingDocumentDate =
                    sepaMandate.grantingDocumentDate != null ? moment(sepaMandate.grantingDocumentDate) : null;
                sepaMandate.revokationDocumentDate =
                    sepaMandate.revokationDocumentDate != null ? moment(sepaMandate.revokationDocumentDate) : null;
                sepaMandate.validFromDate = sepaMandate.validFromDate != null ? moment(sepaMandate.validFromDate) : null;
                sepaMandate.validUntilDate = sepaMandate.validUntilDate != null ? moment(sepaMandate.validUntilDate) : null;
                sepaMandate.lastUsedDate = sepaMandate.lastUsedDate != null ? moment(sepaMandate.lastUsedDate) : null;
            });
        }
        return res;
    }
}
