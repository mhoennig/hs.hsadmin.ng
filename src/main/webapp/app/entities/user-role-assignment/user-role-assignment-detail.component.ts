import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { IUserRoleAssignment } from 'app/shared/model/user-role-assignment.model';

@Component({
    selector: 'jhi-user-role-assignment-detail',
    templateUrl: './user-role-assignment-detail.component.html'
})
export class UserRoleAssignmentDetailComponent implements OnInit {
    userRoleAssignment: IUserRoleAssignment;

    constructor(protected activatedRoute: ActivatedRoute) {}

    ngOnInit() {
        this.activatedRoute.data.subscribe(({ userRoleAssignment }) => {
            this.userRoleAssignment = userRoleAssignment;
        });
    }

    previousState() {
        window.history.back();
    }
}
