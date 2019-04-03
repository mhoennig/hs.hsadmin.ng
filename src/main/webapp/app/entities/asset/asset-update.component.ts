import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { HttpResponse, HttpErrorResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { filter, map } from 'rxjs/operators';
import * as moment from 'moment';
import { JhiAlertService } from 'ng-jhipster';
import { IAsset } from 'app/shared/model/asset.model';
import { AssetService } from './asset.service';
import { IMembership } from 'app/shared/model/membership.model';
import { MembershipService } from 'app/entities/membership';

@Component({
    selector: 'jhi-asset-update',
    templateUrl: './asset-update.component.html'
})
export class AssetUpdateComponent implements OnInit {
    asset: IAsset;
    isSaving: boolean;

    memberships: IMembership[];
    dateDp: any;

    constructor(
        protected jhiAlertService: JhiAlertService,
        protected assetService: AssetService,
        protected membershipService: MembershipService,
        protected activatedRoute: ActivatedRoute
    ) {}

    ngOnInit() {
        this.isSaving = false;
        this.activatedRoute.data.subscribe(({ asset }) => {
            this.asset = asset;
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
        if (this.asset.id !== undefined) {
            this.subscribeToSaveResponse(this.assetService.update(this.asset));
        } else {
            this.subscribeToSaveResponse(this.assetService.create(this.asset));
        }
    }

    protected subscribeToSaveResponse(result: Observable<HttpResponse<IAsset>>) {
        result.subscribe((res: HttpResponse<IAsset>) => this.onSaveSuccess(), (res: HttpErrorResponse) => this.onSaveError());
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
