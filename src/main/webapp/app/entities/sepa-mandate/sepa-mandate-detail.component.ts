import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { ISepaMandate } from 'app/shared/model/sepa-mandate.model';

@Component({
    selector: 'jhi-sepa-mandate-detail',
    templateUrl: './sepa-mandate-detail.component.html'
})
export class SepaMandateDetailComponent implements OnInit {
    sepaMandate: ISepaMandate;

    constructor(protected activatedRoute: ActivatedRoute) {}

    ngOnInit() {
        this.activatedRoute.data.subscribe(({ sepaMandate }) => {
            this.sepaMandate = sepaMandate;
        });
    }

    previousState() {
        window.history.back();
    }
}
