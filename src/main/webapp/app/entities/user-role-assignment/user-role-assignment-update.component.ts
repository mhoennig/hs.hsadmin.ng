import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { HttpResponse, HttpErrorResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { filter, map } from 'rxjs/operators';
import { JhiAlertService } from 'ng-jhipster';
import { IUserRoleAssignment } from 'app/shared/model/user-role-assignment.model';
import { UserRoleAssignmentService } from './user-role-assignment.service';
import { IUser, UserService } from 'app/core';

@Component({
    selector: 'jhi-user-role-assignment-update',
    templateUrl: './user-role-assignment-update.component.html'
})
export class UserRoleAssignmentUpdateComponent implements OnInit {
    userRoleAssignment: IUserRoleAssignment;
    isSaving: boolean;

    users: IUser[];

    constructor(
        protected jhiAlertService: JhiAlertService,
        protected userRoleAssignmentService: UserRoleAssignmentService,
        protected userService: UserService,
        protected activatedRoute: ActivatedRoute
    ) {}

    ngOnInit() {
        this.isSaving = false;
        this.activatedRoute.data.subscribe(({ userRoleAssignment }) => {
            this.userRoleAssignment = userRoleAssignment;
        });
        this.userService
            .query()
            .pipe(
                filter((mayBeOk: HttpResponse<IUser[]>) => mayBeOk.ok),
                map((response: HttpResponse<IUser[]>) => response.body)
            )
            .subscribe((res: IUser[]) => (this.users = res), (res: HttpErrorResponse) => this.onError(res.message));
    }

    previousState() {
        window.history.back();
    }

    save() {
        this.isSaving = true;
        if (this.userRoleAssignment.id !== undefined) {
            this.subscribeToSaveResponse(this.userRoleAssignmentService.update(this.userRoleAssignment));
        } else {
            this.subscribeToSaveResponse(this.userRoleAssignmentService.create(this.userRoleAssignment));
        }
    }

    protected subscribeToSaveResponse(result: Observable<HttpResponse<IUserRoleAssignment>>) {
        result.subscribe((res: HttpResponse<IUserRoleAssignment>) => this.onSaveSuccess(), (res: HttpErrorResponse) => this.onSaveError());
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

    trackUserById(index: number, item: IUser) {
        return item.id;
    }
}
