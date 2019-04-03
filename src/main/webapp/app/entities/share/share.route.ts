import { Injectable } from '@angular/core';
import { HttpResponse } from '@angular/common/http';
import { Resolve, ActivatedRouteSnapshot, RouterStateSnapshot, Routes } from '@angular/router';
import { UserRouteAccessService } from 'app/core';
import { Observable, of } from 'rxjs';
import { filter, map } from 'rxjs/operators';
import { Share } from 'app/shared/model/share.model';
import { ShareService } from './share.service';
import { ShareComponent } from './share.component';
import { ShareDetailComponent } from './share-detail.component';
import { ShareUpdateComponent } from './share-update.component';
import { ShareDeletePopupComponent } from './share-delete-dialog.component';
import { IShare } from 'app/shared/model/share.model';

@Injectable({ providedIn: 'root' })
export class ShareResolve implements Resolve<IShare> {
    constructor(private service: ShareService) {}

    resolve(route: ActivatedRouteSnapshot, state: RouterStateSnapshot): Observable<IShare> {
        const id = route.params['id'] ? route.params['id'] : null;
        if (id) {
            return this.service.find(id).pipe(
                filter((response: HttpResponse<Share>) => response.ok),
                map((share: HttpResponse<Share>) => share.body)
            );
        }
        return of(new Share());
    }
}

export const shareRoute: Routes = [
    {
        path: '',
        component: ShareComponent,
        data: {
            authorities: ['ROLE_USER'],
            pageTitle: 'hsadminNgApp.share.home.title'
        },
        canActivate: [UserRouteAccessService]
    },
    {
        path: ':id/view',
        component: ShareDetailComponent,
        resolve: {
            share: ShareResolve
        },
        data: {
            authorities: ['ROLE_USER'],
            pageTitle: 'hsadminNgApp.share.home.title'
        },
        canActivate: [UserRouteAccessService]
    },
    {
        path: 'new',
        component: ShareUpdateComponent,
        resolve: {
            share: ShareResolve
        },
        data: {
            authorities: ['ROLE_USER'],
            pageTitle: 'hsadminNgApp.share.home.title'
        },
        canActivate: [UserRouteAccessService]
    },
    {
        path: ':id/edit',
        component: ShareUpdateComponent,
        resolve: {
            share: ShareResolve
        },
        data: {
            authorities: ['ROLE_USER'],
            pageTitle: 'hsadminNgApp.share.home.title'
        },
        canActivate: [UserRouteAccessService]
    }
];

export const sharePopupRoute: Routes = [
    {
        path: ':id/delete',
        component: ShareDeletePopupComponent,
        resolve: {
            share: ShareResolve
        },
        data: {
            authorities: ['ROLE_USER'],
            pageTitle: 'hsadminNgApp.share.home.title'
        },
        canActivate: [UserRouteAccessService],
        outlet: 'popup'
    }
];
