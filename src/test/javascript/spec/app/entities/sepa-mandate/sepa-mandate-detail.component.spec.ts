/* tslint:disable max-line-length */
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';

import { HsadminNgTestModule } from '../../../test.module';
import { SepaMandateDetailComponent } from 'app/entities/sepa-mandate/sepa-mandate-detail.component';
import { SepaMandate } from 'app/shared/model/sepa-mandate.model';

describe('Component Tests', () => {
    describe('SepaMandate Management Detail Component', () => {
        let comp: SepaMandateDetailComponent;
        let fixture: ComponentFixture<SepaMandateDetailComponent>;
        const route = ({ data: of({ sepaMandate: new SepaMandate(123) }) } as any) as ActivatedRoute;

        beforeEach(() => {
            TestBed.configureTestingModule({
                imports: [HsadminNgTestModule],
                declarations: [SepaMandateDetailComponent],
                providers: [{ provide: ActivatedRoute, useValue: route }]
            })
                .overrideTemplate(SepaMandateDetailComponent, '')
                .compileComponents();
            fixture = TestBed.createComponent(SepaMandateDetailComponent);
            comp = fixture.componentInstance;
        });

        describe('OnInit', () => {
            it('Should call load all on init', () => {
                // GIVEN

                // WHEN
                comp.ngOnInit();

                // THEN
                expect(comp.sepaMandate).toEqual(jasmine.objectContaining({ id: 123 }));
            });
        });
    });
});
