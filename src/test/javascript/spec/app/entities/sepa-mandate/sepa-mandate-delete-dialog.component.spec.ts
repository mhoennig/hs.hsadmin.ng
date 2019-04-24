/* tslint:disable max-line-length */
import { ComponentFixture, TestBed, inject, fakeAsync, tick } from '@angular/core/testing';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { Observable, of } from 'rxjs';
import { JhiEventManager } from 'ng-jhipster';

import { HsadminNgTestModule } from '../../../test.module';
import { SepaMandateDeleteDialogComponent } from 'app/entities/sepa-mandate/sepa-mandate-delete-dialog.component';
import { SepaMandateService } from 'app/entities/sepa-mandate/sepa-mandate.service';

describe('Component Tests', () => {
    describe('SepaMandate Management Delete Component', () => {
        let comp: SepaMandateDeleteDialogComponent;
        let fixture: ComponentFixture<SepaMandateDeleteDialogComponent>;
        let service: SepaMandateService;
        let mockEventManager: any;
        let mockActiveModal: any;

        beforeEach(() => {
            TestBed.configureTestingModule({
                imports: [HsadminNgTestModule],
                declarations: [SepaMandateDeleteDialogComponent]
            })
                .overrideTemplate(SepaMandateDeleteDialogComponent, '')
                .compileComponents();
            fixture = TestBed.createComponent(SepaMandateDeleteDialogComponent);
            comp = fixture.componentInstance;
            service = fixture.debugElement.injector.get(SepaMandateService);
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
