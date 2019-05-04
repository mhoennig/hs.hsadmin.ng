import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { HttpResponse, HttpErrorResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { filter, map } from 'rxjs/operators';
import { JhiAlertService } from 'ng-jhipster';
import { IShare } from 'app/shared/model/share.model';
import { ShareService } from './share.service';
import { IMembership } from 'app/shared/model/membership.model';
import { MembershipService } from 'app/entities/membership';

@Component({
    selector: 'jhi-share-update',
    templateUrl: './share-update.component.html'
})
export class ShareUpdateComponent implements OnInit {
    share: IShare;
    isSaving: boolean;

    memberships: IMembership[];
    documentDateDp: any;
    valueDateDp: any;

    constructor(
        protected jhiAlertService: JhiAlertService,
        protected shareService: ShareService,
        protected membershipService: MembershipService,
        protected activatedRoute: ActivatedRoute
    ) {}

    ngOnInit() {
        this.isSaving = false;
        this.activatedRoute.data.subscribe(({ share }) => {
            this.share = share;
        });
        this.membershipService
            .query()
            .pipe(
                filter((mayBeOk: HttpResponse<IMembership[]>) => mayBeOk.ok),
                map((response: HttpResponse<IMembership[]>) => response.body)
            )
            .subscribe((res: IMembership[]) => (this.memberships = res), (res: HttpErrorResponse) => this.onError(res.message));
    }

    previousState() {
        window.history.back();
    }

    save() {
        this.isSaving = true;
        if (this.share.id !== undefined) {
            this.subscribeToSaveResponse(this.shareService.update(this.share));
        } else {
            this.subscribeToSaveResponse(this.shareService.create(this.share));
        }
    }

    protected subscribeToSaveResponse(result: Observable<HttpResponse<IShare>>) {
        result.subscribe((res: HttpResponse<IShare>) => this.onSaveSuccess(), (res: HttpErrorResponse) => this.onSaveError());
    }

    protected onSaveSuccess() {
        this.isSaving = false;
        this.previousState();
    }

    protected onSaveError() {
        this.isSaving = false;
    }

    protected onError(errorMessage: string) {
        this.jhiAlertService.error(errorMessage, null, null);
    }

    trackMembershipById(index: number, item: IMembership) {
        return item.id;
    }
}
