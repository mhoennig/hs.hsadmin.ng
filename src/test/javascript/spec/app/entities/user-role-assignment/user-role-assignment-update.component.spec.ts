/* tslint:disable max-line-length */
import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpResponse } from '@angular/common/http';
import { Observable, of } from 'rxjs';

import { HsadminNgTestModule } from '../../../test.module';
import { UserRoleAssignmentUpdateComponent } from 'app/entities/user-role-assignment/user-role-assignment-update.component';
import { UserRoleAssignmentService } from 'app/entities/user-role-assignment/user-role-assignment.service';
import { UserRoleAssignment } from 'app/shared/model/user-role-assignment.model';

describe('Component Tests', () => {
    describe('UserRoleAssignment Management Update Component', () => {
        let comp: UserRoleAssignmentUpdateComponent;
        let fixture: ComponentFixture<UserRoleAssignmentUpdateComponent>;
        let service: UserRoleAssignmentService;

        beforeEach(() => {
            TestBed.configureTestingModule({
                imports: [HsadminNgTestModule],
                declarations: [UserRoleAssignmentUpdateComponent]
            })
                .overrideTemplate(UserRoleAssignmentUpdateComponent, '')
                .compileComponents();

            fixture = TestBed.createComponent(UserRoleAssignmentUpdateComponent);
            comp = fixture.componentInstance;
            service = fixture.debugElement.injector.get(UserRoleAssignmentService);
        });

        describe('save', () => {
            it(
                'Should call update service on save for existing entity',
                fakeAsync(() => {
                    // GIVEN
                    const entity = new UserRoleAssignment(123);
                    spyOn(service, 'update').and.returnValue(of(new HttpResponse({ body: entity })));
                    comp.userRoleAssignment = entity;
                    // WHEN
                    comp.save();
                    tick(); // simulate async

                    // THEN
                    expect(service.update).toHaveBeenCalledWith(entity);
                    expect(comp.isSaving).toEqual(false);
                })
            );

            it(
                'Should call create service on save for new entity',
                fakeAsync(() => {
                    // GIVEN
                    const entity = new UserRoleAssignment();
                    spyOn(service, 'create').and.returnValue(of(new HttpResponse({ body: entity })));
                    comp.userRoleAssignment = entity;
                    // WHEN
                    comp.save();
                    tick(); // simulate async

                    // THEN
                    expect(service.create).toHaveBeenCalledWith(entity);
                    expect(comp.isSaving).toEqual(false);
                })
            );
        });
    });
});
