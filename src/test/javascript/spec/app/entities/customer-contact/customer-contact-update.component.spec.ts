/* tslint:disable max-line-length */
import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpResponse } from '@angular/common/http';
import { Observable, of } from 'rxjs';

import { HsadminNgTestModule } from '../../../test.module';
import { CustomerContactUpdateComponent } from 'app/entities/customer-contact/customer-contact-update.component';
import { CustomerContactService } from 'app/entities/customer-contact/customer-contact.service';
import { CustomerContact } from 'app/shared/model/customer-contact.model';

describe('Component Tests', () => {
    describe('CustomerContact Management Update Component', () => {
        let comp: CustomerContactUpdateComponent;
        let fixture: ComponentFixture<CustomerContactUpdateComponent>;
        let service: CustomerContactService;

        beforeEach(() => {
            TestBed.configureTestingModule({
                imports: [HsadminNgTestModule],
                declarations: [CustomerContactUpdateComponent]
            })
                .overrideTemplate(CustomerContactUpdateComponent, '')
                .compileComponents();

            fixture = TestBed.createComponent(CustomerContactUpdateComponent);
            comp = fixture.componentInstance;
            service = fixture.debugElement.injector.get(CustomerContactService);
        });

        describe('save', () => {
            it('Should call update service on save for existing entity', fakeAsync(() => {
                // GIVEN
                const entity = new CustomerContact(123);
                spyOn(service, 'update').and.returnValue(of(new HttpResponse({ body: entity })));
                comp.customerContact = entity;
                // WHEN
                comp.save();
                tick(); // simulate async

                // THEN
                expect(service.update).toHaveBeenCalledWith(entity);
                expect(comp.isSaving).toEqual(false);
            }));

            it('Should call create service on save for new entity', fakeAsync(() => {
                // GIVEN
                const entity = new CustomerContact();
                spyOn(service, 'create').and.returnValue(of(new HttpResponse({ body: entity })));
                comp.customerContact = entity;
                // WHEN
                comp.save();
                tick(); // simulate async

                // THEN
                expect(service.create).toHaveBeenCalledWith(entity);
                expect(comp.isSaving).toEqual(false);
            }));
        });
    });
});
