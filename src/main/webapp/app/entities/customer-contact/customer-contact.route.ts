import { Injectable } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { Resolve, ActivatedRouteSnapshot, RouterStateSnapshot, Routes } from '@angular/router';
import { UserRouteAccessService } from 'app/core';
import { Observable, of } from 'rxjs';
import { filter, map } from 'rxjs/operators';
import { CustomerContact } from 'app/shared/model/customer-contact.model';
import { CustomerContactService } from './customer-contact.service';
import { CustomerContactComponent } from './customer-contact.component';
import { CustomerContactDetailComponent } from './customer-contact-detail.component';
import { CustomerContactUpdateComponent } from './customer-contact-update.component';
import { CustomerContactDeletePopupComponent } from './customer-contact-delete-dialog.component';
import { ICustomerContact } from 'app/shared/model/customer-contact.model';

@Injectable({ providedIn: 'root' })
export class CustomerContactResolve implements Resolve<ICustomerContact> {
    constructor(private service: CustomerContactService) {}

    resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<ICustomerContact> {
        const id = route.params['id'] ? route.params['id'] : null;
        if (id) {
            return this.service.find(id).pipe(
                filter((response: HttpResponse<CustomerContact>) => response.ok),
                map((customerContact: HttpResponse<CustomerContact>) => customerContact.body)
            );
        }
        return of(new CustomerContact());
    }
}

export const customerContactRoute: Routes = [
    {
        path: '',
        component: CustomerContactComponent,
        data: {
            authorities: ['ROLE_USER'],
            pageTitle: 'hsadminNgApp.customerContact.home.title'
        },
        canActivate: [UserRouteAccessService]
    },
    {
        path: ':id/view',
        component: CustomerContactDetailComponent,
        resolve: {
            customerContact: CustomerContactResolve
        },
        data: {
            authorities: ['ROLE_USER'],
            pageTitle: 'hsadminNgApp.customerContact.home.title'
        },
        canActivate: [UserRouteAccessService]
    },
    {
        path: 'new',
        component: CustomerContactUpdateComponent,
        resolve: {
            customerContact: CustomerContactResolve
        },
        data: {
            authorities: ['ROLE_USER'],
            pageTitle: 'hsadminNgApp.customerContact.home.title'
        },
        canActivate: [UserRouteAccessService]
    },
    {
        path: ':id/edit',
        component: CustomerContactUpdateComponent,
        resolve: {
            customerContact: CustomerContactResolve
        },
        data: {
            authorities: ['ROLE_USER'],
            pageTitle: 'hsadminNgApp.customerContact.home.title'
        },
        canActivate: [UserRouteAccessService]
    }
];

export const customerContactPopupRoute: Routes = [
    {
        path: ':id/delete',
        component: CustomerContactDeletePopupComponent,
        resolve: {
            customerContact: CustomerContactResolve
        },
        data: {
            authorities: ['ROLE_USER'],
            pageTitle: 'hsadminNgApp.customerContact.home.title'
        },
        canActivate: [UserRouteAccessService],
        outlet: 'popup'
    }
];
