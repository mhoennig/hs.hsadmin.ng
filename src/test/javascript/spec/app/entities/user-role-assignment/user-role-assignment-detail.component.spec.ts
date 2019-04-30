/* tslint:disable max-line-length */
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';

import { HsadminNgTestModule } from '../../../test.module';
import { UserRoleAssignmentDetailComponent } from 'app/entities/user-role-assignment/user-role-assignment-detail.component';
import { UserRoleAssignment } from 'app/shared/model/user-role-assignment.model';

describe('Component Tests', () => {
    describe('UserRoleAssignment Management Detail Component', () => {
        let comp: UserRoleAssignmentDetailComponent;
        let fixture: ComponentFixture<UserRoleAssignmentDetailComponent>;
        const route = ({ data: of({ userRoleAssignment: new UserRoleAssignment(123) }) } as any) as ActivatedRoute;

        beforeEach(() => {
            TestBed.configureTestingModule({
                imports: [HsadminNgTestModule],
                declarations: [UserRoleAssignmentDetailComponent],
                providers: [{ provide: ActivatedRoute, useValue: route }]
            })
                .overrideTemplate(UserRoleAssignmentDetailComponent, '')
                .compileComponents();
            fixture = TestBed.createComponent(UserRoleAssignmentDetailComponent);
            comp = fixture.componentInstance;
        });

        describe('OnInit', () => {
            it('Should call load all on init', () => {
                // GIVEN

                // WHEN
                comp.ngOnInit();

                // THEN
                expect(comp.userRoleAssignment).toEqual(jasmine.objectContaining({ id: 123 }));
            });
        });
    });
});
