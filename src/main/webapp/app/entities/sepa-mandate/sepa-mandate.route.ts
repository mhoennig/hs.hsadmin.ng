import { Injectable } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { Resolve, ActivatedRouteSnapshot, RouterStateSnapshot, Routes } from '@angular/router';
import { UserRouteAccessService } from 'app/core';
import { Observable, of } from 'rxjs';
import { filter, map } from 'rxjs/operators';
import { SepaMandate } from 'app/shared/model/sepa-mandate.model';
import { SepaMandateService } from './sepa-mandate.service';
import { SepaMandateComponent } from './sepa-mandate.component';
import { SepaMandateDetailComponent } from './sepa-mandate-detail.component';
import { SepaMandateUpdateComponent } from './sepa-mandate-update.component';
import { SepaMandateDeletePopupComponent } from './sepa-mandate-delete-dialog.component';
import { ISepaMandate } from 'app/shared/model/sepa-mandate.model';

@Injectable({ providedIn: 'root' })
export class SepaMandateResolve implements Resolve<ISepaMandate> {
    constructor(private service: SepaMandateService) {}

    resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<ISepaMandate> {
        const id = route.params['id'] ? route.params['id'] : null;
        if (id) {
            return this.service.find(id).pipe(
                filter((response: HttpResponse<SepaMandate>) => response.ok),
                map((sepaMandate: HttpResponse<SepaMandate>) => sepaMandate.body)
            );
        }
        return of(new SepaMandate());
    }
}

export const sepaMandateRoute: Routes = [
    {
        path: '',
        component: SepaMandateComponent,
        data: {
            authorities: ['ROLE_USER'],
            pageTitle: 'hsadminNgApp.sepaMandate.home.title'
        },
        canActivate: [UserRouteAccessService]
    },
    {
        path: ':id/view',
        component: SepaMandateDetailComponent,
        resolve: {
            sepaMandate: SepaMandateResolve
        },
        data: {
            authorities: ['ROLE_USER'],
            pageTitle: 'hsadminNgApp.sepaMandate.home.title'
        },
        canActivate: [UserRouteAccessService]
    },
    {
        path: 'new',
        component: SepaMandateUpdateComponent,
        resolve: {
            sepaMandate: SepaMandateResolve
        },
        data: {
            authorities: ['ROLE_USER'],
            pageTitle: 'hsadminNgApp.sepaMandate.home.title'
        },
        canActivate: [UserRouteAccessService]
    },
    {
        path: ':id/edit',
        component: SepaMandateUpdateComponent,
        resolve: {
            sepaMandate: SepaMandateResolve
        },
        data: {
            authorities: ['ROLE_USER'],
            pageTitle: 'hsadminNgApp.sepaMandate.home.title'
        },
        canActivate: [UserRouteAccessService]
    }
];

export const sepaMandatePopupRoute: Routes = [
    {
        path: ':id/delete',
        component: SepaMandateDeletePopupComponent,
        resolve: {
            sepaMandate: SepaMandateResolve
        },
        data: {
            authorities: ['ROLE_USER'],
            pageTitle: 'hsadminNgApp.sepaMandate.home.title'
        },
        canActivate: [UserRouteAccessService],
        outlet: 'popup'
    }
];
