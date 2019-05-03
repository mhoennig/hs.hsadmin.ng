import { Component, OnInit, OnDestroy } from '@angular/core';
import { HttpErrorResponse, HttpHeaders, HttpResponse } from '@angular/common/http';
import { Subscription } from 'rxjs';
import { filter, map } from 'rxjs/operators';
import { JhiEventManager, JhiParseLinks, JhiAlertService } from 'ng-jhipster';

import { IUserRoleAssignment } from 'app/shared/model/user-role-assignment.model';
import { AccountService } from 'app/core';

import { ITEMS_PER_PAGE } from 'app/shared';
import { UserRoleAssignmentService } from './user-role-assignment.service';
import { IUser } from 'app/core/user/user.model';
import { UserService } from 'app/core/user/user.service';
import { TableFilter, queryEquals, queryContains } from 'app/shared/util/tablefilter';

@Component({
    selector: 'jhi-user-role-assignment',
    templateUrl: './user-role-assignment.component.html'
})
export class UserRoleAssignmentComponent implements OnInit, OnDestroy {
    userRoleAssignments: IUserRoleAssignment[];
    currentAccount: any;
    eventSubscriber: Subscription;
    itemsPerPage: number;
    links: any;
    page: any;
    predicate: any;
    reverse: any;
    totalItems: number;
    users: IUser[];
    filter: TableFilter<{
        entityTypeId?: string;
        entityObjectId?: string;
        assignedRole?: string;
        userId?: string;
    }>;

    constructor(
        protected userRoleAssignmentService: UserRoleAssignmentService,
        protected userService: UserService,
        protected jhiAlertService: JhiAlertService,
        protected eventManager: JhiEventManager,
        protected parseLinks: JhiParseLinks,
        protected accountService: AccountService
    ) {
        this.userRoleAssignments = [];
        this.itemsPerPage = ITEMS_PER_PAGE;
        this.page = 0;
        this.links = {
            last: 0
        };
        this.predicate = 'id';
        this.reverse = true;
        this.filter = new TableFilter(
            {
                entityTypeId: queryContains,
                entityObjectId: queryEquals,
                assignedRole: queryEquals,
                userId: queryEquals
            },
            500,
            () => {
                this.loadAll();
            }
        );
    }

    loadAll() {
        this.userRoleAssignmentService
            .query({
                ...this.filter.buildQueryCriteria(),
                page: this.page,
                size: this.itemsPerPage,
                sort: this.sort()
            })
            .subscribe(
                (res: HttpResponse<IUserRoleAssignment[]>) => this.paginateUserRoleAssignments(res.body, res.headers),
                (res: HttpErrorResponse) => this.onError(res.message)
            );
    }

    reset() {
        this.page = 0;
        this.userRoleAssignments = [];
        this.loadAll();
    }

    loadPage(page) {
        this.page = page;
        this.loadAll();
    }

    ngOnInit() {
        this.loadAll();
        this.accountService.identity().then(account => {
            this.currentAccount = account;
        });
        this.userService
            .query()
            .pipe(
                filter((mayBeOk: HttpResponse<IUser[]>) => mayBeOk.ok),
                map((response: HttpResponse<IUser[]>) => response.body)
            )
            .subscribe((res: IUser[]) => (this.users = res), (res: HttpErrorResponse) => this.onError(res.message));
        this.registerChangeInUserRoleAssignments();
    }

    ngOnDestroy() {
        this.eventManager.destroy(this.eventSubscriber);
    }

    trackId(index: number, item: { id: number }) {
        return item.id;
    }

    registerChangeInUserRoleAssignments() {
        this.eventSubscriber = this.eventManager.subscribe('userRoleAssignmentListModification', response => this.reset());
    }

    sort() {
        const result = [this.predicate + ',' + (this.reverse ? 'asc' : 'desc')];
        if (this.predicate !== 'id') {
            result.push('id');
        }
        return result;
    }

    protected paginateUserRoleAssignments(data: IUserRoleAssignment[], headers: HttpHeaders) {
        this.links = this.parseLinks.parse(headers.get('link'));
        this.totalItems = parseInt(headers.get('X-Total-Count'), 10);
        this.userRoleAssignments = [];
        for (let i = 0; i < data.length; i++) {
            this.userRoleAssignments.push(data[i]);
        }
    }

    protected onError(errorMessage: string) {
        this.jhiAlertService.error(errorMessage, null, null);
    }
}
