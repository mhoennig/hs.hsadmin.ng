import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';

import { IShare } from 'app/shared/model/share.model';

@Component({
    selector: 'jhi-share-detail',
    templateUrl: './share-detail.component.html'
})
export class ShareDetailComponent implements OnInit {
    share: IShare;

    constructor(protected activatedRoute: ActivatedRoute) {}

    ngOnInit() {
        this.activatedRoute.data.subscribe(({ share }) => {
            this.share = share;
        });
    }

    previousState() {
        window.history.back();
    }
}
