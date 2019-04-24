/* tslint:disable max-line-length */
import { TestBed, getTestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { of } from 'rxjs';
import { take, map } from 'rxjs/operators';
import * as moment from 'moment';
import { DATE_FORMAT } from 'app/shared/constants/input.constants';
import { SepaMandateService } from 'app/entities/sepa-mandate/sepa-mandate.service';
import { ISepaMandate, SepaMandate } from 'app/shared/model/sepa-mandate.model';

describe('Service Tests', () => {
    describe('SepaMandate Service', () => {
        let injector: TestBed;
        let service: SepaMandateService;
        let httpMock: HttpTestingController;
        let elemDefault: ISepaMandate;
        let currentDate: moment.Moment;
        beforeEach(() => {
            TestBed.configureTestingModule({
                imports: [HttpClientTestingModule]
            });
            injector = getTestBed();
            service = injector.get(SepaMandateService);
            httpMock = injector.get(HttpTestingController);
            currentDate = moment();

            elemDefault = new SepaMandate(
                0,
                'AAAAAAA',
                'AAAAAAA',
                'AAAAAAA',
                currentDate,
                currentDate,
                currentDate,
                currentDate,
                currentDate,
                'AAAAAAA'
            );
        });

        describe('Service methods', async () => {
            it('should find an element', async () => {
                const returnedFromService = Object.assign(
                    {
                        grantingDocumentDate: currentDate.format(DATE_FORMAT),
                        revokationDocumentDate: currentDate.format(DATE_FORMAT),
                        validFromDate: currentDate.format(DATE_FORMAT),
                        validUntilDate: currentDate.format(DATE_FORMAT),
                        lastUsedDate: currentDate.format(DATE_FORMAT)
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

            it('should create a SepaMandate', async () => {
                const returnedFromService = Object.assign(
                    {
                        id: 0,
                        grantingDocumentDate: currentDate.format(DATE_FORMAT),
                        revokationDocumentDate: currentDate.format(DATE_FORMAT),
                        validFromDate: currentDate.format(DATE_FORMAT),
                        validUntilDate: currentDate.format(DATE_FORMAT),
                        lastUsedDate: currentDate.format(DATE_FORMAT)
                    },
                    elemDefault
                );
                const expected = Object.assign(
                    {
                        grantingDocumentDate: currentDate,
                        revokationDocumentDate: currentDate,
                        validFromDate: currentDate,
                        validUntilDate: currentDate,
                        lastUsedDate: currentDate
                    },
                    returnedFromService
                );
                service
                    .create(new SepaMandate(null))
                    .pipe(take(1))
                    .subscribe(resp => expect(resp).toMatchObject({ body: expected }));
                const req = httpMock.expectOne({ method: 'POST' });
                req.flush(JSON.stringify(returnedFromService));
            });

            it('should update a SepaMandate', async () => {
                const returnedFromService = Object.assign(
                    {
                        reference: 'BBBBBB',
                        iban: 'BBBBBB',
                        bic: 'BBBBBB',
                        grantingDocumentDate: currentDate.format(DATE_FORMAT),
                        revokationDocumentDate: currentDate.format(DATE_FORMAT),
                        validFromDate: currentDate.format(DATE_FORMAT),
                        validUntilDate: currentDate.format(DATE_FORMAT),
                        lastUsedDate: currentDate.format(DATE_FORMAT),
                        remark: 'BBBBBB'
                    },
                    elemDefault
                );

                const expected = Object.assign(
                    {
                        grantingDocumentDate: currentDate,
                        revokationDocumentDate: currentDate,
                        validFromDate: currentDate,
                        validUntilDate: currentDate,
                        lastUsedDate: currentDate
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

            it('should return a list of SepaMandate', async () => {
                const returnedFromService = Object.assign(
                    {
                        reference: 'BBBBBB',
                        iban: 'BBBBBB',
                        bic: 'BBBBBB',
                        grantingDocumentDate: currentDate.format(DATE_FORMAT),
                        revokationDocumentDate: currentDate.format(DATE_FORMAT),
                        validFromDate: currentDate.format(DATE_FORMAT),
                        validUntilDate: currentDate.format(DATE_FORMAT),
                        lastUsedDate: currentDate.format(DATE_FORMAT),
                        remark: 'BBBBBB'
                    },
                    elemDefault
                );
                const expected = Object.assign(
                    {
                        grantingDocumentDate: currentDate,
                        revokationDocumentDate: currentDate,
                        validFromDate: currentDate,
                        validUntilDate: currentDate,
                        lastUsedDate: currentDate
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

            it('should delete a SepaMandate', async () => {
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
