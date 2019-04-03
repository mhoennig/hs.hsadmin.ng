import { NgModule, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { RouterModule } from '@angular/router';
import { JhiLanguageService } from 'ng-jhipster';
import { JhiLanguageHelper } from 'app/core';

import { HsadminNgSharedModule } from 'app/shared';
import {
    ShareComponent,
    ShareDetailComponent,
    ShareUpdateComponent,
    ShareDeletePopupComponent,
    ShareDeleteDialogComponent,
    shareRoute,
    sharePopupRoute
} from './';

const ENTITY_STATES = [...shareRoute, ...sharePopupRoute];

@NgModule({
    imports: [HsadminNgSharedModule, RouterModule.forChild(ENTITY_STATES)],
    declarations: [ShareComponent, ShareDetailComponent, ShareUpdateComponent, ShareDeleteDialogComponent, ShareDeletePopupComponent],
    entryComponents: [ShareComponent, ShareUpdateComponent, ShareDeleteDialogComponent, ShareDeletePopupComponent],
    providers: [{ provide: JhiLanguageService, useClass: JhiLanguageService }],
    schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class HsadminNgShareModule {
    constructor(private languageService: JhiLanguageService, private languageHelper: JhiLanguageHelper) {
        this.languageHelper.language.subscribe((languageKey: string) => {
            if (languageKey !== undefined) {
                this.languageService.changeLanguage(languageKey);
            }
        });
    }
}
