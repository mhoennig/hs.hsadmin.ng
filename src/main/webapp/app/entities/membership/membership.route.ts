import { Injectable } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { Resolve, ActivatedRouteSnapshot, RouterStateSnapshot, Routes } from '@angular/router';
import { UserRouteAccessService } from 'app/core';
import { Observable, of } from 'rxjs';
import { filter, map } from 'rxjs/operators';
import { Membership } from 'app/shared/model/membership.model';
import { MembershipService } from './membership.service';
import { MembershipComponent } from './membership.component';
import { MembershipDetailComponent } from './membership-detail.component';
import { MembershipUpdateComponent } from './membership-update.component';
import { MembershipDeletePopupComponent } from './membership-delete-dialog.component';
import { IMembership } from 'app/shared/model/membership.model';

@Injectable({ providedIn: 'root' })
export class MembershipResolve implements Resolve<IMembership> {
    constructor(private service: MembershipService) {}

    resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<IMembership> {
        const id = route.params['id'] ? route.params['id'] : null;
        if (id) {
            return this.service.find(id).pipe(
                filter((response: HttpResponse<Membership>) => response.ok),
                map((membership: HttpResponse<Membership>) => membership.body)
            );
        }
        return of(new Membership());
    }
}

export const membershipRoute: Routes = [
    {
        path: '',
        component: MembershipComponent,
        data: {
            authorities: ['ROLE_USER'],
            pageTitle: 'hsadminNgApp.membership.home.title'
        },
        canActivate: [UserRouteAccessService]
    },
    {
        path: ':id/view',
        component: MembershipDetailComponent,
        resolve: {
            membership: MembershipResolve
        },
        data: {
            authorities: ['ROLE_USER'],
            pageTitle: 'hsadminNgApp.membership.home.title'
        },
        canActivate: [UserRouteAccessService]
    },
    {
        path: 'new',
        component: MembershipUpdateComponent,
        resolve: {
            membership: MembershipResolve
        },
        data: {
            authorities: ['ROLE_USER'],
            pageTitle: 'hsadminNgApp.membership.home.title'
        },
        canActivate: [UserRouteAccessService]
    },
    {
        path: ':id/edit',
        component: MembershipUpdateComponent,
        resolve: {
            membership: MembershipResolve
        },
        data: {
            authorities: ['ROLE_USER'],
            pageTitle: 'hsadminNgApp.membership.home.title'
        },
        canActivate: [UserRouteAccessService]
    }
];

export const membershipPopupRoute: Routes = [
    {
        path: ':id/delete',
        component: MembershipDeletePopupComponent,
        resolve: {
            membership: MembershipResolve
        },
        data: {
            authorities: ['ROLE_USER'],
            pageTitle: 'hsadminNgApp.membership.home.title'
        },
        canActivate: [UserRouteAccessService],
        outlet: 'popup'
    }
];
