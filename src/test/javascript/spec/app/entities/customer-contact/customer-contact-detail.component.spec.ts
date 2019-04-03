/* tslint:disable max-line-length */
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';

import { HsadminNgTestModule } from '../../../test.module';
import { CustomerContactDetailComponent } from 'app/entities/customer-contact/customer-contact-detail.component';
import { CustomerContact } from 'app/shared/model/customer-contact.model';

describe('Component Tests', () => {
    describe('CustomerContact Management Detail Component', () => {
        let comp: CustomerContactDetailComponent;
        let fixture: ComponentFixture<CustomerContactDetailComponent>;
        const route = ({ data: of({ customerContact: new CustomerContact(123) }) } as any) as ActivatedRoute;

        beforeEach(() => {
            TestBed.configureTestingModule({
                imports: [HsadminNgTestModule],
                declarations: [CustomerContactDetailComponent],
                providers: [{ provide: ActivatedRoute, useValue: route }]
            })
                .overrideTemplate(CustomerContactDetailComponent, '')
                .compileComponents();
            fixture = TestBed.createComponent(CustomerContactDetailComponent);
            comp = fixture.componentInstance;
        });

        describe('OnInit', () => {
            it('Should call load all on init', () => {
                // GIVEN

                // WHEN
                comp.ngOnInit();

                // THEN
                expect(comp.customerContact).toEqual(jasmine.objectContaining({ id: 123 }));
            });
        });
    });
});
