import { Component, OnInit, OnDestroy } from '@angular/core';
import { HttpErrorResponse, HttpHeaders, HttpResponse } from '@angular/common/http';
import { Subscription, Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { JhiEventManager, JhiParseLinks, JhiAlertService } from 'ng-jhipster';

import { ICustomer } from 'app/shared/model/customer.model';
import { AccountService } from 'app/core';

import { ITEMS_PER_PAGE } from 'app/shared';
import { CustomerService } from './customer.service';

@Component({
    selector: 'jhi-customer',
    templateUrl: './customer.component.html'
})
export class CustomerComponent implements OnInit, OnDestroy {
    customers: ICustomer[];
    currentAccount: any;
    eventSubscriber: Subscription;
    itemsPerPage: number;
    links: any;
    page: any;
    predicate: any;
    reverse: any;
    totalItems: number;
    filterValue: any;
    filterValueChanged = new Subject<string>();
    subscription: Subscription;

    constructor(
        protected customerService: CustomerService,
        protected jhiAlertService: JhiAlertService,
        protected eventManager: JhiEventManager,
        protected parseLinks: JhiParseLinks,
        protected accountService: AccountService
    ) {
        this.customers = [];
        this.itemsPerPage = ITEMS_PER_PAGE;
        this.page = 0;
        this.links = {
            last: 0
        };
        this.predicate = 'id';
        this.reverse = true;
        this.resetFilter();
    }

    resetFilter() {
        this.filterValue = {
            number: null,
            prefix: null
        };
        this.loadAll();
    }

    loadAll() {
        let criteria = {
            ...(this.filterValue.number && { 'number.equals': this.filterValue.number }),
            ...(this.filterValue.prefix && { 'prefix.contains': this.filterValue.prefix })
        };
        this.customerService
            .query({
                ...criteria,
                page: this.page,
                size: this.itemsPerPage,
                sort: this.sort()
            })
            .subscribe(
                (res: HttpResponse<ICustomer[]>) => this.paginateCustomers(res.body, res.headers),
                (res: HttpErrorResponse) => this.onError(res.message)
            );
    }

    filter($event) {
        this.filterValueChanged.next($event.target.value);
    }

    reset() {
        this.page = 0;
        this.customers = [];
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
        this.registerChangeInCustomers();

        this.subscription = this.filterValueChanged
            .pipe(
                debounceTime(500),
                distinctUntilChanged((previous: any, current: any) => previous === current)
            )
            .subscribe(() => {
                this.loadAll();
            });
    }

    ngOnDestroy() {
        this.eventManager.destroy(this.eventSubscriber);
    }

    trackId(index: number, item: ICustomer) {
        return item.id;
    }

    registerChangeInCustomers() {
        this.eventSubscriber = this.eventManager.subscribe('customerListModification', response => this.reset());
    }

    sort() {
        const result = [this.predicate + ',' + (this.reverse ? 'asc' : 'desc')];
        if (this.predicate !== 'id') {
            result.push('id');
        }
        return result;
    }

    protected paginateCustomers(data: ICustomer[], headers: HttpHeaders) {
        this.links = this.parseLinks.parse(headers.get('link'));
        this.totalItems = parseInt(headers.get('X-Total-Count'), 10);
        this.page = 0;
        this.customers = [];
        for (let i = 0; i < data.length; i++) {
            this.customers.push(data[i]);
        }
    }

    protected onError(errorMessage: string) {
        this.jhiAlertService.error(errorMessage, null, null);
    }
}
