import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { HttpResponse, HttpErrorResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { filter, map } from 'rxjs/operators';
import * as moment from 'moment';
import { JhiAlertService } from 'ng-jhipster';
import { IMembership } from 'app/shared/model/membership.model';
import { MembershipService } from './membership.service';
import { ICustomer } from 'app/shared/model/customer.model';
import { CustomerService } from 'app/entities/customer';

@Component({
    selector: 'jhi-membership-update',
    templateUrl: './membership-update.component.html'
})
export class MembershipUpdateComponent implements OnInit {
    membership: IMembership;
    isSaving: boolean;

    customers: ICustomer[];
    fromDp: any;
    toDp: any;

    constructor(
        protected jhiAlertService: JhiAlertService,
        protected membershipService: MembershipService,
        protected customerService: CustomerService,
        protected activatedRoute: ActivatedRoute
    ) {}

    ngOnInit() {
        this.isSaving = false;
        this.activatedRoute.data.subscribe(({ membership }) => {
            this.membership = membership;
        });
        this.customerService
            .query()
            .pipe(
                filter((mayBeOk: HttpResponse<ICustomer[]>) => mayBeOk.ok),
                map((response: HttpResponse<ICustomer[]>) => response.body)
            )
            .subscribe((res: ICustomer[]) => (this.customers = res), (res: HttpErrorResponse) => this.onError(res.message));
    }

    previousState() {
        window.history.back();
    }

    save() {
        this.isSaving = true;
        if (this.membership.id !== undefined) {
            this.subscribeToSaveResponse(this.membershipService.update(this.membership));
        } else {
            this.subscribeToSaveResponse(this.membershipService.create(this.membership));
        }
    }

    protected subscribeToSaveResponse(result: Observable<HttpResponse<IMembership>>) {
        result.subscribe((res: HttpResponse<IMembership>) => this.onSaveSuccess(), (res: HttpErrorResponse) => this.onSaveError());
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

    trackCustomerById(index: number, item: ICustomer) {
        return item.id;
    }
}
