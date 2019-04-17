import { NgModule, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { RouterModule } from '@angular/router';
import { JhiLanguageService } from 'ng-jhipster';
import { JhiLanguageHelper } from 'app/core';

import { HsadminNgSharedModule } from 'app/shared';
import {
    SepaMandateComponent,
    SepaMandateDetailComponent,
    SepaMandateUpdateComponent,
    SepaMandateDeletePopupComponent,
    SepaMandateDeleteDialogComponent,
    sepaMandateRoute,
    sepaMandatePopupRoute
} from './';

const ENTITY_STATES = [...sepaMandateRoute, ...sepaMandatePopupRoute];

@NgModule({
    imports: [HsadminNgSharedModule, RouterModule.forChild(ENTITY_STATES)],
    declarations: [
        SepaMandateComponent,
        SepaMandateDetailComponent,
        SepaMandateUpdateComponent,
        SepaMandateDeleteDialogComponent,
        SepaMandateDeletePopupComponent
    ],
    entryComponents: [SepaMandateComponent, SepaMandateUpdateComponent, SepaMandateDeleteDialogComponent, SepaMandateDeletePopupComponent],
    providers: [{ provide: JhiLanguageService, useClass: JhiLanguageService }],
    schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class HsadminNgSepaMandateModule {
    constructor(private languageService: JhiLanguageService, private languageHelper: JhiLanguageHelper) {
        this.languageHelper.language.subscribe((languageKey: string) => {
            if (languageKey !== undefined) {
                this.languageService.changeLanguage(languageKey);
            }
        });
    }
}
