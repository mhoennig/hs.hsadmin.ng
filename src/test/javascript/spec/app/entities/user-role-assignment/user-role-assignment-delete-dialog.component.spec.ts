/* tslint:disable max-line-length */
import { ComponentFixture, TestBed, inject, fakeAsync, tick } from '@angular/core/testing';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { Observable, of } from 'rxjs';
import { JhiEventManager } from 'ng-jhipster';

import { HsadminNgTestModule } from '../../../test.module';
import { UserRoleAssignmentDeleteDialogComponent } from 'app/entities/user-role-assignment/user-role-assignment-delete-dialog.component';
import { UserRoleAssignmentService } from 'app/entities/user-role-assignment/user-role-assignment.service';

describe('Component Tests', () => {
    describe('UserRoleAssignment Management Delete Component', () => {
        let comp: UserRoleAssignmentDeleteDialogComponent;
        let fixture: ComponentFixture<UserRoleAssignmentDeleteDialogComponent>;
        let service: UserRoleAssignmentService;
        let mockEventManager: any;
        let mockActiveModal: any;

        beforeEach(() => {
            TestBed.configureTestingModule({
                imports: [HsadminNgTestModule],
                declarations: [UserRoleAssignmentDeleteDialogComponent]
            })
                .overrideTemplate(UserRoleAssignmentDeleteDialogComponent, '')
                .compileComponents();
            fixture = TestBed.createComponent(UserRoleAssignmentDeleteDialogComponent);
            comp = fixture.componentInstance;
            service = fixture.debugElement.injector.get(UserRoleAssignmentService);
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
