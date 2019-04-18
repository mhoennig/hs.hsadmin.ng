import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { HttpResponse, HttpErrorResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { filter, map } from 'rxjs/operators';
import * as moment from 'moment';
import { JhiAlertService } from 'ng-jhipster';
import { ISepaMandate } from 'app/shared/model/sepa-mandate.model';
import { SepaMandateService } from './sepa-mandate.service';
import { ICustomer } from 'app/shared/model/customer.model';
import { CustomerService } from 'app/entities/customer';

@Component({
    selector: 'jhi-sepa-mandate-update',
    templateUrl: './sepa-mandate-update.component.html'
})
export class SepaMandateUpdateComponent implements OnInit {
    sepaMandate: ISepaMandate;
    isSaving: boolean;

    customers: ICustomer[];
    documentDateDp: any;
    validFromDp: any;
    validUntilDp: any;
    lastUsedDp: any;
    cancellationDateDp: any;

    constructor(
        protected jhiAlertService: JhiAlertService,
        protected sepaMandateService: SepaMandateService,
        protected customerService: CustomerService,
        protected activatedRoute: ActivatedRoute
    ) {}

    ngOnInit() {
        this.isSaving = false;
        this.activatedRoute.data.subscribe(({ sepaMandate }) => {
            this.sepaMandate = sepaMandate;
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
        if (this.sepaMandate.id !== undefined) {
            this.subscribeToSaveResponse(this.sepaMandateService.update(this.sepaMandate));
        } else {
            this.subscribeToSaveResponse(this.sepaMandateService.create(this.sepaMandate));
        }
    }

    protected subscribeToSaveResponse(result: Observable<HttpResponse<ISepaMandate>>) {
        result.subscribe((res: HttpResponse<ISepaMandate>) => this.onSaveSuccess(), (res: HttpErrorResponse) => this.onSaveError());
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
