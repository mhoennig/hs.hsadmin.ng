import { Component, OnInit, OnDestroy } from '@angular/core';
import { HttpErrorResponse, HttpHeaders, HttpResponse } from '@angular/common/http';
import { Subscription } from 'rxjs';
import { filter, map } from 'rxjs/operators';
import { JhiEventManager, JhiParseLinks, JhiAlertService } from 'ng-jhipster';

import { IShare } from 'app/shared/model/share.model';
import { AccountService } from 'app/core';

import { ITEMS_PER_PAGE } from 'app/shared';
import { ShareService } from './share.service';
import { IMembership } from 'app/shared/model/membership.model';
import { MembershipService } from 'app/entities/membership';
import { TableFilter, queryYearAsDateRange, queryEquals } from 'app/shared/util/tablefilter';

@Component({
    selector: 'jhi-share',
    templateUrl: './share.component.html'
})
export class ShareComponent implements OnInit, OnDestroy {
    shares: IShare[];
    currentAccount: any;
    eventSubscriber: Subscription;
    itemsPerPage: number;
    links: any;
    page: any;
    predicate: any;
    reverse: any;
    totalItems: number;
    memberships: IMembership[];
    filter: TableFilter<{
        documentDate?: string;
        valueDate?: string;
        action?: string;
        quantity?: string;
        membershipId?: string;
    }>;

    constructor(
        protected shareService: ShareService,
        protected membershipService: MembershipService,
        protected jhiAlertService: JhiAlertService,
        protected eventManager: JhiEventManager,
        protected parseLinks: JhiParseLinks,
        protected accountService: AccountService
    ) {
        this.shares = [];
        this.itemsPerPage = ITEMS_PER_PAGE;
        this.page = 0;
        this.links = {
            last: 0
        };
        this.predicate = 'id';
        this.reverse = true;
        this.filter = new TableFilter(
            {
                documentDate: queryYearAsDateRange,
                valueDate: queryYearAsDateRange,
                action: queryEquals,
                quantity: queryEquals,
                membershipId: queryEquals
            },
            500,
            () => {
                this.loadAll();
            }
        );
    }

    loadAll() {
        this.shareService
            .query({
                ...this.filter.buildQueryCriteria(),
                page: this.page,
                size: this.itemsPerPage,
                sort: this.sort()
            })
            .subscribe(
                (res: HttpResponse<IShare[]>) => this.paginateShares(res.body, res.headers),
                (res: HttpErrorResponse) => this.onError(res.message)
            );
    }

    reset() {
        this.page = 0;
        this.shares = [];
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
        this.membershipService
            .query()
            .pipe(
                filter((mayBeOk: HttpResponse<IMembership[]>) => mayBeOk.ok),
                map((response: HttpResponse<IMembership[]>) => response.body)
            )
            .subscribe((res: IMembership[]) => (this.memberships = res), (res: HttpErrorResponse) => this.onError(res.message));
        this.registerChangeInShares();
    }

    ngOnDestroy() {
        this.eventManager.destroy(this.eventSubscriber);
    }

    trackId(index: number, item: { id: number }) {
        return item.id;
    }

    registerChangeInShares() {
        this.eventSubscriber = this.eventManager.subscribe('shareListModification', response => this.reset());
    }

    sort() {
        const result = [this.predicate + ',' + (this.reverse ? 'asc' : 'desc')];
        if (this.predicate !== 'id') {
            result.push('id');
        }
        return result;
    }

    protected paginateShares(data: IShare[], headers: HttpHeaders) {
        this.links = this.parseLinks.parse(headers.get('link'));
        this.totalItems = parseInt(headers.get('X-Total-Count'), 10);
        this.page = 0;
        this.shares = [];
        for (let i = 0; i < data.length; i++) {
            this.shares.push(data[i]);
        }
    }

    protected onError(errorMessage: string) {
        this.jhiAlertService.error(errorMessage, null, null);
    }
}
