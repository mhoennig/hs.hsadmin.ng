/* tslint:disable max-line-length */
import { ComponentFixture, TestBed, inject, fakeAsync, tick } from '@angular/core/testing';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { Observable, of } from 'rxjs';
import { JhiEventManager } from 'ng-jhipster';

import { HsadminNgTestModule } from '../../../test.module';
import { MembershipDeleteDialogComponent } from 'app/entities/membership/membership-delete-dialog.component';
import { MembershipService } from 'app/entities/membership/membership.service';

describe('Component Tests', () => {
    describe('Membership Management Delete Component', () => {
        let comp: MembershipDeleteDialogComponent;
        let fixture: ComponentFixture<MembershipDeleteDialogComponent>;
        let service: MembershipService;
        let mockEventManager: any;
        let mockActiveModal: any;

        beforeEach(() => {
            TestBed.configureTestingModule({
                imports: [HsadminNgTestModule],
                declarations: [MembershipDeleteDialogComponent]
            })
                .overrideTemplate(MembershipDeleteDialogComponent, '')
                .compileComponents();
            fixture = TestBed.createComponent(MembershipDeleteDialogComponent);
            comp = fixture.componentInstance;
            service = fixture.debugElement.injector.get(MembershipService);
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
