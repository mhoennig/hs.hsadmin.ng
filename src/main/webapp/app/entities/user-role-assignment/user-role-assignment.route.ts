import { Injectable } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { Resolve, ActivatedRouteSnapshot, RouterStateSnapshot, Routes } from '@angular/router';
import { UserRouteAccessService } from 'app/core';
import { Observable, of } from 'rxjs';
import { filter, map } from 'rxjs/operators';
import { UserRoleAssignment } from 'app/shared/model/user-role-assignment.model';
import { UserRoleAssignmentService } from './user-role-assignment.service';
import { UserRoleAssignmentComponent } from './user-role-assignment.component';
import { UserRoleAssignmentDetailComponent } from './user-role-assignment-detail.component';
import { UserRoleAssignmentUpdateComponent } from './user-role-assignment-update.component';
import { UserRoleAssignmentDeletePopupComponent } from './user-role-assignment-delete-dialog.component';
import { IUserRoleAssignment } from 'app/shared/model/user-role-assignment.model';

@Injectable({ providedIn: 'root' })
export class UserRoleAssignmentResolve implements Resolve<IUserRoleAssignment> {
    constructor(private service: UserRoleAssignmentService) {}

    resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<IUserRoleAssignment> {
        const id = route.params['id'] ? route.params['id'] : null;
        if (id) {
            return this.service.find(id).pipe(
                filter((response: HttpResponse<UserRoleAssignment>) => response.ok),
                map((userRoleAssignment: HttpResponse<UserRoleAssignment>) => userRoleAssignment.body)
            );
        }
        return of(new UserRoleAssignment());
    }
}

export const userRoleAssignmentRoute: Routes = [
    {
        path: '',
        component: UserRoleAssignmentComponent,
        data: {
            authorities: ['ROLE_USER'],
            pageTitle: 'hsadminNgApp.userRoleAssignment.home.title'
        },
        canActivate: [UserRouteAccessService]
    },
    {
        path: ':id/view',
        component: UserRoleAssignmentDetailComponent,
        resolve: {
            userRoleAssignment: UserRoleAssignmentResolve
        },
        data: {
            authorities: ['ROLE_USER'],
            pageTitle: 'hsadminNgApp.userRoleAssignment.home.title'
        },
        canActivate: [UserRouteAccessService]
    },
    {
        path: 'new',
        component: UserRoleAssignmentUpdateComponent,
        resolve: {
            userRoleAssignment: UserRoleAssignmentResolve
        },
        data: {
            authorities: ['ROLE_USER'],
            pageTitle: 'hsadminNgApp.userRoleAssignment.home.title'
        },
        canActivate: [UserRouteAccessService]
    },
    {
        path: ':id/edit',
        component: UserRoleAssignmentUpdateComponent,
        resolve: {
            userRoleAssignment: UserRoleAssignmentResolve
        },
        data: {
            authorities: ['ROLE_USER'],
            pageTitle: 'hsadminNgApp.userRoleAssignment.home.title'
        },
        canActivate: [UserRouteAccessService]
    }
];

export const userRoleAssignmentPopupRoute: Routes = [
    {
        path: ':id/delete',
        component: UserRoleAssignmentDeletePopupComponent,
        resolve: {
            userRoleAssignment: UserRoleAssignmentResolve
        },
        data: {
            authorities: ['ROLE_USER'],
            pageTitle: 'hsadminNgApp.userRoleAssignment.home.title'
        },
        canActivate: [UserRouteAccessService],
        outlet: 'popup'
    }
];
