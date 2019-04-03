/* tslint:disable max-line-length */
import { ComponentFixture, TestBed, inject, fakeAsync, tick } from '@angular/core/testing';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { Observable, of } from 'rxjs';
import { JhiEventManager } from 'ng-jhipster';

import { HsadminNgTestModule } from '../../../test.module';
import { CustomerContactDeleteDialogComponent } from 'app/entities/customer-contact/customer-contact-delete-dialog.component';
import { CustomerContactService } from 'app/entities/customer-contact/customer-contact.service';

describe('Component Tests', () => {
    describe('CustomerContact Management Delete Component', () => {
        let comp: CustomerContactDeleteDialogComponent;
        let fixture: ComponentFixture<CustomerContactDeleteDialogComponent>;
        let service: CustomerContactService;
        let mockEventManager: any;
        let mockActiveModal: any;

        beforeEach(() => {
            TestBed.configureTestingModule({
                imports: [HsadminNgTestModule],
                declarations: [CustomerContactDeleteDialogComponent]
            })
                .overrideTemplate(CustomerContactDeleteDialogComponent, '')
                .compileComponents();
            fixture = TestBed.createComponent(CustomerContactDeleteDialogComponent);
            comp = fixture.componentInstance;
            service = fixture.debugElement.injector.get(CustomerContactService);
            mockEventManager = fixture.debugElement.injector.get(JhiEventManager);
            mockActiveModal = fixture.debugElement.injector.get(NgbActiveModal);
        });

        describe('confirmDelete', () => {
            it('Should call delete service on confirmDelete', inject(
                [],
                fakeAsync(() => {
                    // GIVEN
                    spyOn(service, 'delete').and.returnValue(of({}));

                    // WHEN
                    comp.confirmDelete(123);
                    tick();

                    // THEN
                    expect(service.delete).toHaveBeenCalledWith(123);
                    expect(mockActiveModal.dismissSpy).toHaveBeenCalled();
                    expect(mockEventManager.broadcastSpy).toHaveBeenCalled();
                })
            ));
        });
    });
});
