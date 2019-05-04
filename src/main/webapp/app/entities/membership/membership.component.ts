import { Component, OnInit, OnDestroy } from '@angular/core';
import { HttpErrorResponse, HttpHeaders, HttpResponse } from '@angular/common/http';
import { Subscription } from 'rxjs';
import { filter, map } from 'rxjs/operators';
import { JhiEventManager, JhiParseLinks, JhiAlertService } from 'ng-jhipster';

import { IMembership } from 'app/shared/model/membership.model';
import { AccountService } from 'app/core';

import { ITEMS_PER_PAGE } from 'app/shared';
import { MembershipService } from './membership.service';
import { ICustomer } from 'app/shared/model/customer.model';
import { CustomerService } from 'app/entities/customer';
import { TableFilter, queryYearAsDateRange, queryEquals } from 'app/shared/util/tablefilter';

@Component({
    selector: 'jhi-membership',
    templateUrl: './membership.component.html'
})
export class MembershipComponent implements OnInit, OnDestroy {
    memberships: IMembership[];
    currentAccount: any;
    eventSubscriber: Subscription;
    itemsPerPage: number;
    links: any;
    page: any;
    predicate: any;
    reverse: any;
    totalItems: number;
    customers: ICustomer[];
    filter: TableFilter<{
        admissionDocumentDate?: string;
        cancellationDocumentDate?: string;
        memberFromDate?: string;
        memberUntilDate?: string;
        customerId?: string;
    }>;

    constructor(
        protected membershipService: MembershipService,
        protected customerService: CustomerService,
        protected jhiAlertService: JhiAlertService,
        protected eventManager: JhiEventManager,
        protected parseLinks: JhiParseLinks,
        protected accountService: AccountService
    ) {
        this.memberships = [];
        this.itemsPerPage = ITEMS_PER_PAGE;
        this.page = 0;
        this.links = {
            last: 0
        };
        this.predicate = 'id';
        this.reverse = true;
        this.filter = new TableFilter(
            {
                admissionDocumentDate: queryYearAsDateRange,
                cancellationDocumentDate: queryYearAsDateRange,
                memberFromDate: queryYearAsDateRange,
                memberUntilDate: queryYearAsDateRange,
                customerId: queryEquals
            },
            500,
            () => {
                this.reset();
            }
        );
    }

    loadAll() {
        this.membershipService
            .query({
                ...this.filter.buildQueryCriteria(),
                page: this.page,
                size: this.itemsPerPage,
                sort: this.sort()
            })
            .subscribe(
                (res: HttpResponse<IMembership[]>) => this.paginateMemberships(res.body, res.headers),
                (res: HttpErrorResponse) => this.onError(res.message)
            );
    }

    reset() {
        this.page = 0;
        this.memberships = [];
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
        this.registerChangeInMemberships();
        this.customerService
            .query()
            .pipe(
                filter((mayBeOk: HttpResponse<IMembership[]>) => mayBeOk.ok),
                map((response: HttpResponse<IMembership[]>) => response.body)
            )
            .subscribe((res: IMembership[]) => (this.customers = res), (res: HttpErrorResponse) => this.onError(res.message));
    }

    ngOnDestroy() {
        this.eventManager.destroy(this.eventSubscriber);
    }

    trackId(index: number, item: { id: number }) {
        return item.id;
    }

    registerChangeInMemberships() {
        this.eventSubscriber = this.eventManager.subscribe('membershipListModification', response => this.reset());
    }

    sort() {
        const result = [this.predicate + ',' + (this.reverse ? 'asc' : 'desc')];
        if (this.predicate !== 'id') {
            result.push('id');
        }
        return result;
    }

    protected paginateMemberships(data: IMembership[], headers: HttpHeaders) {
        this.links = this.parseLinks.parse(headers.get('link'));
        this.totalItems = parseInt(headers.get('X-Total-Count'), 10);
        this.memberships = [];
        for (let i = 0; i < data.length; i++) {
            this.memberships.push(data[i]);
        }
    }

    protected onError(errorMessage: string) {
        this.jhiAlertService.error(errorMessage, null, null);
    }
}
