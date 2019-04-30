import { NgModule, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { RouterModule } from '@angular/router';
import { JhiLanguageService } from 'ng-jhipster';
import { JhiLanguageHelper } from 'app/core';

import { HsadminNgSharedModule } from 'app/shared';
import {
    UserRoleAssignmentComponent,
    UserRoleAssignmentDetailComponent,
    UserRoleAssignmentUpdateComponent,
    UserRoleAssignmentDeletePopupComponent,
    UserRoleAssignmentDeleteDialogComponent,
    userRoleAssignmentRoute,
    userRoleAssignmentPopupRoute
} from './';

const ENTITY_STATES = [...userRoleAssignmentRoute, ...userRoleAssignmentPopupRoute];

@NgModule({
    imports: [HsadminNgSharedModule, RouterModule.forChild(ENTITY_STATES)],
    declarations: [
        UserRoleAssignmentComponent,
        UserRoleAssignmentDetailComponent,
        UserRoleAssignmentUpdateComponent,
        UserRoleAssignmentDeleteDialogComponent,
        UserRoleAssignmentDeletePopupComponent
    ],
    entryComponents: [
        UserRoleAssignmentComponent,
        UserRoleAssignmentUpdateComponent,
        UserRoleAssignmentDeleteDialogComponent,
        UserRoleAssignmentDeletePopupComponent
    ],
    providers: [{ provide: JhiLanguageService, useClass: JhiLanguageService }],
    schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class HsadminNgUserRoleAssignmentModule {
    constructor(private languageService: JhiLanguageService, private languageHelper: JhiLanguageHelper) {
        this.languageHelper.language.subscribe((languageKey: string) => {
            if (languageKey !== undefined) {
                this.languageService.changeLanguage(languageKey);
            }
        });
    }
}
