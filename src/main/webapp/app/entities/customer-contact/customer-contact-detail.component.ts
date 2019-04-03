import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { ICustomerContact } from 'app/shared/model/customer-contact.model';

@Component({
    selector: 'jhi-customer-contact-detail',
    templateUrl: './customer-contact-detail.component.html'
})
export class CustomerContactDetailComponent implements OnInit {
    customerContact: ICustomerContact;

    constructor(protected activatedRoute: ActivatedRoute) {}

    ngOnInit() {
        this.activatedRoute.data.subscribe(({ customerContact }) => {
            this.customerContact = customerContact;
        });
    }

    previousState() {
        window.history.back();
    }
}
