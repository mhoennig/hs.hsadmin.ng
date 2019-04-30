import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';

import { NgbActiveModal, NgbModal, NgbModalRef } from '@ng-bootstrap/ng-bootstrap';
import { JhiEventManager } from 'ng-jhipster';

import { IUserRoleAssignment } from 'app/shared/model/user-role-assignment.model';
import { UserRoleAssignmentService } from './user-role-assignment.service';

@Component({
    selector: 'jhi-user-role-assignment-delete-dialog',
    templateUrl: './user-role-assignment-delete-dialog.component.html'
})
export class UserRoleAssignmentDeleteDialogComponent {
    userRoleAssignment: IUserRoleAssignment;

    constructor(
        protected userRoleAssignmentService: UserRoleAssignmentService,
        public activeModal: NgbActiveModal,
        protected eventManager: JhiEventManager
    ) {}

    clear() {
        this.activeModal.dismiss('cancel');
    }

    confirmDelete(id: number) {
        this.userRoleAssignmentService.delete(id).subscribe(response => {
            this.eventManager.broadcast({
                name: 'userRoleAssignmentListModification',
                content: 'Deleted an userRoleAssignment'
            });
            this.activeModal.dismiss(true);
        });
    }
}

@Component({
    selector: 'jhi-user-role-assignment-delete-popup',
    template: ''
})
export class UserRoleAssignmentDeletePopupComponent implements OnInit, OnDestroy {
    protected ngbModalRef: NgbModalRef;

    constructor(protected activatedRoute: ActivatedRoute, protected router: Router, protected modalService: NgbModal) {}

    ngOnInit() {
        this.activatedRoute.data.subscribe(({ userRoleAssignment }) => {
            setTimeout(() => {
                this.ngbModalRef = this.modalService.open(UserRoleAssignmentDeleteDialogComponent as Component, {
                    size: 'lg',
                    backdrop: 'static'
                });
                this.ngbModalRef.componentInstance.userRoleAssignment = userRoleAssignment;
                this.ngbModalRef.result.then(
                    result => {
                        this.router.navigate(['/user-role-assignment', { outlets: { popup: null } }]);
                        this.ngbModalRef = null;
                    },
                    reason => {
                        this.router.navigate(['/user-role-assignment', { outlets: { popup: null } }]);
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
