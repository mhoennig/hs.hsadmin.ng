import { Injectable } from '@angular/core';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

import { SERVER_API_URL } from 'app/app.constants';
import { createRequestOption } from 'app/shared';
import { IUserRoleAssignment } from 'app/shared/model/user-role-assignment.model';

type EntityResponseType = HttpResponse<IUserRoleAssignment>;
type EntityArrayResponseType = HttpResponse<IUserRoleAssignment[]>;

@Injectable({ providedIn: 'root' })
export class UserRoleAssignmentService {
    public resourceUrl = SERVER_API_URL + 'api/user-role-assignments';

    constructor(protected http: HttpClient) {}

    create(userRoleAssignment: IUserRoleAssignment): Observable<EntityResponseType> {
        return this.http.post<IUserRoleAssignment>(this.resourceUrl, userRoleAssignment, { observe: 'response' });
    }

    update(userRoleAssignment: IUserRoleAssignment): Observable<EntityResponseType> {
        return this.http.put<IUserRoleAssignment>(this.resourceUrl, userRoleAssignment, { observe: 'response' });
    }

    find(id: number): Observable<EntityResponseType> {
        return this.http.get<IUserRoleAssignment>(`${this.resourceUrl}/${id}`, { observe: 'response' });
    }

    query(req?: any): Observable<EntityArrayResponseType> {
        const options = createRequestOption(req);
        return this.http.get<IUserRoleAssignment[]>(this.resourceUrl, { params: options, observe: 'response' });
    }

    delete(id: number): Observable<HttpResponse<any>> {
        return this.http.delete<any>(`${this.resourceUrl}/${id}`, { observe: 'response' });
    }
}
