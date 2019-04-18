import { CUSTOM_ELEMENTS_SCHEMA, NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';

@NgModule({
    imports: [
        RouterModule.forChild([
            {
                path: 'customer',
                loadChildren: './customer/customer.module#HsadminNgCustomerModule'
            },
            {
                path: 'membership',
                loadChildren: './membership/membership.module#HsadminNgMembershipModule'
            },
            {
                path: 'share',
                loadChildren: './share/share.module#HsadminNgShareModule'
            },
            {
                path: 'asset',
                loadChildren: './asset/asset.module#HsadminNgAssetModule'
            },
            {
                path: 'sepa-mandate',
                loadChildren: './sepa-mandate/sepa-mandate.module#HsadminNgSepaMandateModule'
            }
            /* jhipster-needle-add-entity-route - JHipster will add entity modules routes here */
        ])
    ],
    declarations: [],
    entryComponents: [],
    providers: [],
    schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class HsadminNgEntityModule {}
