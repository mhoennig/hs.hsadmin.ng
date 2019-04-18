/* tslint:disable max-line-length */
import { TestBed, getTestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { of } from 'rxjs';
import { take, map } from 'rxjs/operators';
import * as moment from 'moment';
import { DATE_FORMAT } from 'app/shared/constants/input.constants';
import { ShareService } from 'app/entities/share/share.service';
import { IShare, Share, ShareAction } from 'app/shared/model/share.model';

describe('Service Tests', () => {
    describe('Share Service', () => {
        let injector: TestBed;
        let service: ShareService;
        let httpMock: HttpTestingController;
        let elemDefault: IShare;
        let currentDate: moment.Moment;
        beforeEach(() => {
            TestBed.configureTestingModule({
                imports: [HttpClientTestingModule]
            });
            injector = getTestBed();
            service = injector.get(ShareService);
            httpMock = injector.get(HttpTestingController);
            currentDate = moment();

            elemDefault = new Share(0, currentDate, currentDate, ShareAction.SUBSCRIPTION, 0, 'AAAAAAA');
        });

        describe('Service methods', async () => {
            it('should find an element', async () => {
                const returnedFromService = Object.assign(
                    {
                        documentDate: currentDate.format(DATE_FORMAT),
                        valueDate: currentDate.format(DATE_FORMAT)
                    },
                    elemDefault
                );
                service
                    .find(123)
                    .pipe(take(1))
                    .subscribe(resp => expect(resp).toMatchObject({ body: elemDefault }));

                const req = httpMock.expectOne({ method: 'GET' });
                req.flush(JSON.stringify(returnedFromService));
            });

            it('should create a Share', async () => {
                const returnedFromService = Object.assign(
                    {
                        id: 0,
                        documentDate: currentDate.format(DATE_FORMAT),
                        valueDate: currentDate.format(DATE_FORMAT)
                    },
                    elemDefault
                );
                const expected = Object.assign(
                    {
                        documentDate: currentDate,
                        valueDate: currentDate
                    },
                    returnedFromService
                );
                service
                    .create(new Share(null))
                    .pipe(take(1))
                    .subscribe(resp => expect(resp).toMatchObject({ body: expected }));
                const req = httpMock.expectOne({ method: 'POST' });
                req.flush(JSON.stringify(returnedFromService));
            });

            it('should update a Share', async () => {
                const returnedFromService = Object.assign(
                    {
                        documentDate: currentDate.format(DATE_FORMAT),
                        valueDate: currentDate.format(DATE_FORMAT),
                        action: 'BBBBBB',
                        quantity: 1,
                        remark: 'BBBBBB'
                    },
                    elemDefault
                );

                const expected = Object.assign(
                    {
                        documentDate: currentDate,
                        valueDate: currentDate
                    },
                    returnedFromService
                );
                service
                    .update(expected)
                    .pipe(take(1))
                    .subscribe(resp => expect(resp).toMatchObject({ body: expected }));
                const req = httpMock.expectOne({ method: 'PUT' });
                req.flush(JSON.stringify(returnedFromService));
            });

            it('should return a list of Share', async () => {
                const returnedFromService = Object.assign(
                    {
                        documentDate: currentDate.format(DATE_FORMAT),
                        valueDate: currentDate.format(DATE_FORMAT),
                        action: 'BBBBBB',
                        quantity: 1,
                        remark: 'BBBBBB'
                    },
                    elemDefault
                );
                const expected = Object.assign(
                    {
                        documentDate: currentDate,
                        valueDate: currentDate
                    },
                    returnedFromService
                );
                service
                    .query(expected)
                    .pipe(
                        take(1),
                        map(resp => resp.body)
                    )
                    .subscribe(body => expect(body).toContainEqual(expected));
                const req = httpMock.expectOne({ method: 'GET' });
                req.flush(JSON.stringify([returnedFromService]));
                httpMock.verify();
            });

            it('should delete a Share', async () => {
                const rxPromise = service.delete(123).subscribe(resp => expect(resp.ok));

                const req = httpMock.expectOne({ method: 'DELETE' });
                req.flush({ status: 200 });
            });
        });

        afterEach(() => {
            httpMock.verify();
        });
    });
});
