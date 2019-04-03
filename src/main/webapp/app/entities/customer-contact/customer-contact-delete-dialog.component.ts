import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { NgbActiveModal, NgbModal, NgbModalRef } from '@ng-bootstrap/ng-bootstrap';
import { JhiEventManager } from 'ng-jhipster';

import { ICustomerContact } from 'app/shared/model/customer-contact.model';
import { CustomerContactService } from './customer-contact.service';

@Component({
    selector: 'jhi-customer-contact-delete-dialog',
    templateUrl: './customer-contact-delete-dialog.component.html'
})
export class CustomerContactDeleteDialogComponent {
    customerContact: ICustomerContact;

    constructor(
        protected customerContactService: CustomerContactService,
        public activeModal: NgbActiveModal,
        protected eventManager: JhiEventManager
    ) {}

    clear() {
        this.activeModal.dismiss('cancel');
    }

    confirmDelete(id: number) {
        this.customerContactService.delete(id).subscribe(response => {
            this.eventManager.broadcast({
                name: 'customerContactListModification',
                content: 'Deleted an customerContact'
            });
            this.activeModal.dismiss(true);
        });
    }
}

@Component({
    selector: 'jhi-customer-contact-delete-popup',
    template: ''
})
export class CustomerContactDeletePopupComponent implements OnInit, OnDestroy {
    protected ngbModalRef: NgbModalRef;

    constructor(protected activatedRoute: ActivatedRoute, protected router: Router, protected modalService: NgbModal) {}

    ngOnInit() {
        this.activatedRoute.data.subscribe(({ customerContact }) => {
            setTimeout(() => {
                this.ngbModalRef = this.modalService.open(CustomerContactDeleteDialogComponent as Component, {
                    size: 'lg',
                    backdrop: 'static'
                });
                this.ngbModalRef.componentInstance.customerContact = customerContact;
                this.ngbModalRef.result.then(
                    result => {
                        this.router.navigate(['/customer-contact', { outlets: { popup: null } }]);
                        this.ngbModalRef = null;
                    },
                    reason => {
                        this.router.navigate(['/customer-contact', { outlets: { popup: null } }]);
                        this.ngbModalRef = null;
                    }
                );
            }, 0);
        });
    }

    ngOnDestroy() {
        this.ngbModalRef = null;
    }
}
