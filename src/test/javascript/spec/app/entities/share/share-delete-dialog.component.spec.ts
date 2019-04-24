/* tslint:disable max-line-length */
import { ComponentFixture, TestBed, inject, fakeAsync, tick } from '@angular/core/testing';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { Observable, of } from 'rxjs';
import { JhiEventManager } from 'ng-jhipster';

import { HsadminNgTestModule } from '../../../test.module';
import { ShareDeleteDialogComponent } from 'app/entities/share/share-delete-dialog.component';
import { ShareService } from 'app/entities/share/share.service';

describe('Component Tests', () => {
    describe('Share Management Delete Component', () => {
        let comp: ShareDeleteDialogComponent;
        let fixture: ComponentFixture<ShareDeleteDialogComponent>;
        let service: ShareService;
        let mockEventManager: any;
        let mockActiveModal: any;

        beforeEach(() => {
            TestBed.configureTestingModule({
                imports: [HsadminNgTestModule],
                declarations: [ShareDeleteDialogComponent]
            })
                .overrideTemplate(ShareDeleteDialogComponent, '')
                .compileComponents();
            fixture = TestBed.createComponent(ShareDeleteDialogComponent);
            comp = fixture.componentInstance;
            service = fixture.debugElement.injector.get(ShareService);
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
