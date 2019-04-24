import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import * as moment from 'moment';
import { DATE_FORMAT } from 'app/shared/constants/input.constants';
import { map } from 'rxjs/operators';

import { SERVER_API_URL } from 'app/app.constants';
import { createRequestOption } from 'app/shared';
import { IMembership } from 'app/shared/model/membership.model';

type EntityResponseType = HttpResponse<IMembership>;
type EntityArrayResponseType = HttpResponse<IMembership[]>;

@Injectable({ providedIn: 'root' })
export class MembershipService {
    public resourceUrl = SERVER_API_URL + 'api/memberships';

    constructor(protected http: HttpClient) {}

    create(membership: IMembership): Observable<EntityResponseType> {
        const copy = this.convertDateFromClient(membership);
        return this.http
            .post<IMembership>(this.resourceUrl, copy, { observe: 'response' })
            .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
    }

    update(membership: IMembership): Observable<EntityResponseType> {
        const copy = this.convertDateFromClient(membership);
        return this.http
            .put<IMembership>(this.resourceUrl, copy, { observe: 'response' })
            .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
    }

    find(id: number): Observable<EntityResponseType> {
        return this.http
            .get<IMembership>(`${this.resourceUrl}/${id}`, { observe: 'response' })
            .pipe(map((res: EntityResponseType) => this.convertDateFromServer(res)));
    }

    query(req?: any): Observable<EntityArrayResponseType> {
        const options = createRequestOption(req);
        return this.http
            .get<IMembership[]>(this.resourceUrl, { params: options, observe: 'response' })
            .pipe(map((res: EntityArrayResponseType) => this.convertDateArrayFromServer(res)));
    }

    delete(id: number): Observable<HttpResponse<any>> {
        return this.http.delete<any>(`${this.resourceUrl}/${id}`, { observe: 'response' });
    }

    protected convertDateFromClient(membership: IMembership): IMembership {
        const copy: IMembership = Object.assign({}, membership, {
            admissionDocumentDate:
                membership.admissionDocumentDate != null && membership.admissionDocumentDate.isValid()
                    ? membership.admissionDocumentDate.format(DATE_FORMAT)
                    : null,
            cancellationDocumentDate:
                membership.cancellationDocumentDate != null && membership.cancellationDocumentDate.isValid()
                    ? membership.cancellationDocumentDate.format(DATE_FORMAT)
                    : null,
            memberFromDate:
                membership.memberFromDate != null && membership.memberFromDate.isValid()
                    ? membership.memberFromDate.format(DATE_FORMAT)
                    : null,
            memberUntilDate:
                membership.memberUntilDate != null && membership.memberUntilDate.isValid()
                    ? membership.memberUntilDate.format(DATE_FORMAT)
                    : null
        });
        return copy;
    }

    protected convertDateFromServer(res: EntityResponseType): EntityResponseType {
        if (res.body) {
            res.body.admissionDocumentDate = res.body.admissionDocumentDate != null ? moment(res.body.admissionDocumentDate) : null;
            res.body.cancellationDocumentDate =
                res.body.cancellationDocumentDate != null ? moment(res.body.cancellationDocumentDate) : null;
            res.body.memberFromDate = res.body.memberFromDate != null ? moment(res.body.memberFromDate) : null;
            res.body.memberUntilDate = res.body.memberUntilDate != null ? moment(res.body.memberUntilDate) : null;
        }
        return res;
    }

    protected convertDateArrayFromServer(res: EntityArrayResponseType): EntityArrayResponseType {
        if (res.body) {
            res.body.forEach((membership: IMembership) => {
                membership.admissionDocumentDate =
                    membership.admissionDocumentDate != null ? moment(membership.admissionDocumentDate) : null;
                membership.cancellationDocumentDate =
                    membership.cancellationDocumentDate != null ? moment(membership.cancellationDocumentDate) : null;
                membership.memberFromDate = membership.memberFromDate != null ? moment(membership.memberFromDate) : null;
                membership.memberUntilDate = membership.memberUntilDate != null ? moment(membership.memberUntilDate) : null;
            });
        }
        return res;
    }
}
