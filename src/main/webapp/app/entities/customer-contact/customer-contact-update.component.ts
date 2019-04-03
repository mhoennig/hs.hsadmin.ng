import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { HttpResponse, HttpErrorResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { filter, map } from 'rxjs/operators';
import { JhiAlertService } from 'ng-jhipster';
import { ICustomerContact } from 'app/shared/model/customer-contact.model';
import { CustomerContactService } from './customer-contact.service';
import { IContact } from 'app/shared/model/contact.model';
import { ContactService } from 'app/entities/contact';
import { ICustomer } from 'app/shared/model/customer.model';
import { CustomerService } from 'app/entities/customer';

@Component({
    selector: 'jhi-customer-contact-update',
    templateUrl: './customer-contact-update.component.html'
})
export class CustomerContactUpdateComponent implements OnInit {
    customerContact: ICustomerContact;
    isSaving: boolean;

    contacts: IContact[];

    customers: ICustomer[];

    constructor(
        protected jhiAlertService: JhiAlertService,
        protected customerContactService: CustomerContactService,
        protected contactService: ContactService,
        protected customerService: CustomerService,
        protected activatedRoute: ActivatedRoute
    ) {}

    ngOnInit() {
        this.isSaving = false;
        this.activatedRoute.data.subscribe(({ customerContact }) => {
            this.customerContact = customerContact;
        });
        this.contactService
            .query()
            .pipe(
                filter((mayBeOk: HttpResponse<IContact[]>) => mayBeOk.ok),
                map((response: HttpResponse<IContact[]>) => response.body)
            )
            .subscribe((res: IContact[]) => (this.contacts = res), (res: HttpErrorResponse) => this.onError(res.message));
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
        if (this.customerContact.id !== undefined) {
            this.subscribeToSaveResponse(this.customerContactService.update(this.customerContact));
        } else {
            this.subscribeToSaveResponse(this.customerContactService.create(this.customerContact));
        }
    }

    protected subscribeToSaveResponse(result: Observable<HttpResponse<ICustomerContact>>) {
        result.subscribe((res: HttpResponse<ICustomerContact>) => this.onSaveSuccess(), (res: HttpErrorResponse) => this.onSaveError());
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

    trackContactById(index: number, item: IContact) {
        return item.id;
    }

    trackCustomerById(index: number, item: ICustomer) {
        return item.id;
    }
}
