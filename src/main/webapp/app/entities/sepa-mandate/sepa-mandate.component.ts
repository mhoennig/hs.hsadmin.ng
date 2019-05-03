import { Component, OnInit, OnDestroy } from '@angular/core';
import { HttpErrorResponse, HttpHeaders, HttpResponse } from '@angular/common/http';
import { Subscription } from 'rxjs';
import { filter, map } from 'rxjs/operators';
import { JhiEventManager, JhiParseLinks, JhiAlertService } from 'ng-jhipster';

import { ISepaMandate } from 'app/shared/model/sepa-mandate.model';
import { AccountService } from 'app/core';

import { ITEMS_PER_PAGE } from 'app/shared';
import { SepaMandateService } from './sepa-mandate.service';
import { ICustomer } from 'app/shared/model/customer.model';
import { CustomerService } from 'app/entities/customer';
import { TableFilter, queryYearAsDateRange, queryEquals, queryContains } from 'app/shared/util/tablefilter';
import { IMembership } from 'app/shared/model/membership.model';

@Component({
    selector: 'jhi-sepa-mandate',
    templateUrl: './sepa-mandate.component.html'
})
export class SepaMandateComponent implements OnInit, OnDestroy {
    sepaMandates: ISepaMandate[];
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
        reference?: string;
        iban?: string;
        bic?: string;
        grantingDocumentDate?: string;
        revokationDocumentDate?: string;
        validFromDate?: string;
        validUntilDate?: string;
        lastUsedDate?: string;
        customerId?: string;
    }>;

    constructor(
        protected sepaMandateService: SepaMandateService,
        protected customerService: CustomerService,
        protected jhiAlertService: JhiAlertService,
        protected eventManager: JhiEventManager,
        protected parseLinks: JhiParseLinks,
        protected accountService: AccountService
    ) {
        this.sepaMandates = [];
        this.itemsPerPage = ITEMS_PER_PAGE;
        this.page = 0;
        this.links = {
            last: 0
        };
        this.predicate = 'id';
        this.reverse = true;
        this.filter = new TableFilter(
            {
                reference: queryContains,
                iban: queryContains,
                bic: queryContains,
                grantingDocumentDate: queryYearAsDateRange,
                revokationDocumentDate: queryYearAsDateRange,
                validFromDate: queryYearAsDateRange,
                validUntilDate: queryYearAsDateRange,
                lastUsedDate: queryYearAsDateRange,
                customerId: queryEquals
            },
            500,
            () => {
                this.loadAll();
            }
        );
    }

    loadAll() {
        this.sepaMandateService
            .query({
                ...this.filter.buildQueryCriteria(),
                page: this.page,
                size: this.itemsPerPage,
                sort: this.sort()
            })
            .subscribe(
                (res: HttpResponse<ISepaMandate[]>) => this.paginateSepaMandates(res.body, res.headers),
                (res: HttpErrorResponse) => this.onError(res.message)
            );
    }

    reset() {
        this.page = 0;
        this.sepaMandates = [];
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
        this.registerChangeInSepaMandates();
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

    registerChangeInSepaMandates() {
        this.eventSubscriber = this.eventManager.subscribe('sepaMandateListModification', response => this.reset());
    }

    sort() {
        const result = [this.predicate + ',' + (this.reverse ? 'asc' : 'desc')];
        if (this.predicate !== 'id') {
            result.push('id');
        }
        return result;
    }

    protected paginateSepaMandates(data: ISepaMandate[], headers: HttpHeaders) {
        this.links = this.parseLinks.parse(headers.get('link'));
        this.totalItems = parseInt(headers.get('X-Total-Count'), 10);
        this.page = 0;
        this.sepaMandates = [];
        for (let i = 0; i < data.length; i++) {
            this.sepaMandates.push(data[i]);
        }
    }

    protected onError(errorMessage: string) {
        this.jhiAlertService.error(errorMessage, null, null);
    }
}
