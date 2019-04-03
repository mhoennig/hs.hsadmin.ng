import { NgModule, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { RouterModule } from '@angular/router';

@NgModule({
    imports: [
        RouterModule.forChild([
            {
                path: 'customer',
                loadChildren: './customer/customer.module#HsadminNgCustomerModule'
            },
            {
                path: 'contact',
                loadChildren: './contact/contact.module#HsadminNgContactModule'
            },
            {
                path: 'customer-contact',
                loadChildren: './customer-contact/customer-contact.module#HsadminNgCustomerContactModule'
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
