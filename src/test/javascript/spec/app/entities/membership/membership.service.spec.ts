/* tslint:disable max-line-length */
import { TestBed, getTestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { of } from 'rxjs';
import { take, map } from 'rxjs/operators';
import * as moment from 'moment';
import { DATE_FORMAT } from 'app/shared/constants/input.constants';
import { MembershipService } from 'app/entities/membership/membership.service';
import { IMembership, Membership } from 'app/shared/model/membership.model';

describe('Service Tests', () => {
    describe('Membership Service', () => {
        let injector: TestBed;
        let service: MembershipService;
        let httpMock: HttpTestingController;
        let elemDefault: IMembership;
        let currentDate: moment.Moment;
        beforeEach(() => {
            TestBed.configureTestingModule({
                imports: [HttpClientTestingModule]
            });
            injector = getTestBed();
            service = injector.get(MembershipService);
            httpMock = injector.get(HttpTestingController);
            currentDate = moment();

            elemDefault = new Membership(0, currentDate, currentDate, currentDate, 'AAAAAAA');
        });

        describe('Service methods', async () => {
            it('should find an element', async () => {
                const returnedFromService = Object.assign(
                    {
                        documentDate: currentDate.format(DATE_FORMAT),
                        memberFrom: currentDate.format(DATE_FORMAT),
                        memberUntil: currentDate.format(DATE_FORMAT)
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

            it('should create a Membership', async () => {
                const returnedFromService = Object.assign(
                    {
                        id: 0,
                        documentDate: currentDate.format(DATE_FORMAT),
                        memberFrom: currentDate.format(DATE_FORMAT),
                        memberUntil: currentDate.format(DATE_FORMAT)
                    },
                    elemDefault
                );
                const expected = Object.assign(
                    {
                        documentDate: currentDate,
                        memberFrom: currentDate,
                        memberUntil: currentDate
                    },
                    returnedFromService
                );
                service
                    .create(new Membership(null))
                    .pipe(take(1))
                    .subscribe(resp => expect(resp).toMatchObject({ body: expected }));
                const req = httpMock.expectOne({ method: 'POST' });
                req.flush(JSON.stringify(returnedFromService));
            });

            it('should update a Membership', async () => {
                const returnedFromService = Object.assign(
                    {
                        documentDate: currentDate.format(DATE_FORMAT),
                        memberFrom: currentDate.format(DATE_FORMAT),
                        memberUntil: currentDate.format(DATE_FORMAT),
                        remark: 'BBBBBB'
                    },
                    elemDefault
                );

                const expected = Object.assign(
                    {
                        documentDate: currentDate,
                        memberFrom: currentDate,
                        memberUntil: currentDate
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

            it('should return a list of Membership', async () => {
                const returnedFromService = Object.assign(
                    {
                        documentDate: currentDate.format(DATE_FORMAT),
                        memberFrom: currentDate.format(DATE_FORMAT),
                        memberUntil: currentDate.format(DATE_FORMAT),
                        remark: 'BBBBBB'
                    },
                    elemDefault
                );
                const expected = Object.assign(
                    {
                        documentDate: currentDate,
                        memberFrom: currentDate,
                        memberUntil: currentDate
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

            it('should delete a Membership', async () => {
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
