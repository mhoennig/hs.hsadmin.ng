import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { IMembership } from 'app/shared/model/membership.model';

@Component({
    selector: 'jhi-membership-detail',
    templateUrl: './membership-detail.component.html'
})
export class MembershipDetailComponent implements OnInit {
    membership: IMembership;

    constructor(protected activatedRoute: ActivatedRoute) {}

    ngOnInit() {
        this.activatedRoute.data.subscribe(({ membership }) => {
            this.membership = membership;
        });
    }

    previousState() {
        window.history.back();
    }
}
