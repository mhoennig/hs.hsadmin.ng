/* tslint:disable max-line-length */
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';

import { HsadminNgTestModule } from '../../../test.module';
import { MembershipDetailComponent } from 'app/entities/membership/membership-detail.component';
import { Membership } from 'app/shared/model/membership.model';

describe('Component Tests', () => {
    describe('Membership Management Detail Component', () => {
        let comp: MembershipDetailComponent;
        let fixture: ComponentFixture<MembershipDetailComponent>;
        const route = ({ data: of({ membership: new Membership(123) }) } as any) as ActivatedRoute;

        beforeEach(() => {
            TestBed.configureTestingModule({
                imports: [HsadminNgTestModule],
                declarations: [MembershipDetailComponent],
                providers: [{ provide: ActivatedRoute, useValue: route }]
            })
                .overrideTemplate(MembershipDetailComponent, '')
                .compileComponents();
            fixture = TestBed.createComponent(MembershipDetailComponent);
            comp = fixture.componentInstance;
        });

        describe('OnInit', () => {
            it('Should call load all on init', () => {
                // GIVEN

                // WHEN
                comp.ngOnInit();

                // THEN
                expect(comp.membership).toEqual(jasmine.objectContaining({ id: 123 }));
            });
        });
    });
});
