import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { NgbActiveModal, NgbModal, NgbModalRef } from '@ng-bootstrap/ng-bootstrap';
import { JhiEventManager } from 'ng-jhipster';

import { ISepaMandate } from 'app/shared/model/sepa-mandate.model';
import { SepaMandateService } from './sepa-mandate.service';

@Component({
    selector: 'jhi-sepa-mandate-delete-dialog',
    templateUrl: './sepa-mandate-delete-dialog.component.html'
})
export class SepaMandateDeleteDialogComponent {
    sepaMandate: ISepaMandate;

    constructor(
        protected sepaMandateService: SepaMandateService,
        public activeModal: NgbActiveModal,
        protected eventManager: JhiEventManager
    ) {}

    clear() {
        this.activeModal.dismiss('cancel');
    }

    confirmDelete(id: number) {
        this.sepaMandateService.delete(id).subscribe(response => {
            this.eventManager.broadcast({
                name: 'sepaMandateListModification',
                content: 'Deleted an sepaMandate'
            });
            this.activeModal.dismiss(true);
        });
    }
}

@Component({
    selector: 'jhi-sepa-mandate-delete-popup',
    template: ''
})
export class SepaMandateDeletePopupComponent implements OnInit, OnDestroy {
    protected ngbModalRef: NgbModalRef;

    constructor(protected activatedRoute: ActivatedRoute, protected router: Router, protected modalService: NgbModal) {}

    ngOnInit() {
        this.activatedRoute.data.subscribe(({ sepaMandate }) => {
            setTimeout(() => {
                this.ngbModalRef = this.modalService.open(SepaMandateDeleteDialogComponent as Component, {
                    size: 'lg',
                    backdrop: 'static'
                });
                this.ngbModalRef.componentInstance.sepaMandate = sepaMandate;
                this.ngbModalRef.result.then(
                    result => {
                        this.router.navigate(['/sepa-mandate', { outlets: { popup: null } }]);
                        this.ngbModalRef = null;
                    },
                    reason => {
                        this.router.navigate(['/sepa-mandate', { outlets: { popup: null } }]);
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
