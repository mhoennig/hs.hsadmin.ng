import { NgModule, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { RouterModule } from '@angular/router';
import { JhiLanguageService } from 'ng-jhipster';
import { JhiLanguageHelper } from 'app/core';

import { HsadminNgSharedModule } from 'app/shared';
import {
    CustomerContactComponent,
    CustomerContactDetailComponent,
    CustomerContactUpdateComponent,
    CustomerContactDeletePopupComponent,
    CustomerContactDeleteDialogComponent,
    customerContactRoute,
    customerContactPopupRoute
} from './';

const ENTITY_STATES = [...customerContactRoute, ...customerContactPopupRoute];

@NgModule({
    imports: [HsadminNgSharedModule, RouterModule.forChild(ENTITY_STATES)],
    declarations: [
        CustomerContactComponent,
        CustomerContactDetailComponent,
        CustomerContactUpdateComponent,
        CustomerContactDeleteDialogComponent,
        CustomerContactDeletePopupComponent
    ],
    entryComponents: [
        CustomerContactComponent,
        CustomerContactUpdateComponent,
        CustomerContactDeleteDialogComponent,
        CustomerContactDeletePopupComponent
    ],
    providers: [{ provide: JhiLanguageService, useClass: JhiLanguageService }],
    schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class HsadminNgCustomerContactModule {
    constructor(private languageService: JhiLanguageService, private languageHelper: JhiLanguageHelper) {
        this.languageHelper.language.subscribe((languageKey: string) => {
            if (languageKey !== undefined) {
                this.languageService.changeLanguage(languageKey);
            }
        });
    }
}
