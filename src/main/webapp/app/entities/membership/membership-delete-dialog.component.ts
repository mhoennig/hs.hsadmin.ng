import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { NgbActiveModal, NgbModal, NgbModalRef } from '@ng-bootstrap/ng-bootstrap';
import { JhiEventManager } from 'ng-jhipster';

import { IMembership } from 'app/shared/model/membership.model';
import { MembershipService } from './membership.service';

@Component({
    selector: 'jhi-membership-delete-dialog',
    templateUrl: './membership-delete-dialog.component.html'
})
export class MembershipDeleteDialogComponent {
    membership: IMembership;

    constructor(
        protected membershipService: MembershipService,
        public activeModal: NgbActiveModal,
        protected eventManager: JhiEventManager
    ) {}

    clear() {
        this.activeModal.dismiss('cancel');
    }

    confirmDelete(id: number) {
        this.membershipService.delete(id).subscribe(response => {
            this.eventManager.broadcast({
                name: 'membershipListModification',
                content: 'Deleted an membership'
            });
            this.activeModal.dismiss(true);
        });
    }
}

@Component({
    selector: 'jhi-membership-delete-popup',
    template: ''
})
export class MembershipDeletePopupComponent implements OnInit, OnDestroy {
    protected ngbModalRef: NgbModalRef;

    constructor(protected activatedRoute: ActivatedRoute, protected router: Router, protected modalService: NgbModal) {}

    ngOnInit() {
        this.activatedRoute.data.subscribe(({ membership }) => {
            setTimeout(() => {
                this.ngbModalRef = this.modalService.open(MembershipDeleteDialogComponent as Component, { size: 'lg', backdrop: 'static' });
                this.ngbModalRef.componentInstance.membership = membership;
                this.ngbModalRef.result.then(
                    result => {
                        this.router.navigate(['/membership', { outlets: { popup: null } }]);
                        this.ngbModalRef = null;
                    },
                    reason => {
                        this.router.navigate(['/membership', { outlets: { popup: null } }]);
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
