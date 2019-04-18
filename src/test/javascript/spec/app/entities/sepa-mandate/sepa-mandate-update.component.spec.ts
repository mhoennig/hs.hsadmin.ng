/* tslint:disable max-line-length */
import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpResponse } from '@angular/common/http';
import { Observable, of } from 'rxjs';

import { HsadminNgTestModule } from '../../../test.module';
import { SepaMandateUpdateComponent } from 'app/entities/sepa-mandate/sepa-mandate-update.component';
import { SepaMandateService } from 'app/entities/sepa-mandate/sepa-mandate.service';
import { SepaMandate } from 'app/shared/model/sepa-mandate.model';

describe('Component Tests', () => {
    describe('SepaMandate Management Update Component', () => {
        let comp: SepaMandateUpdateComponent;
        let fixture: ComponentFixture<SepaMandateUpdateComponent>;
        let service: SepaMandateService;

        beforeEach(() => {
            TestBed.configureTestingModule({
                imports: [HsadminNgTestModule],
                declarations: [SepaMandateUpdateComponent]
            })
                .overrideTemplate(SepaMandateUpdateComponent, '')
                .compileComponents();

            fixture = TestBed.createComponent(SepaMandateUpdateComponent);
            comp = fixture.componentInstance;
            service = fixture.debugElement.injector.get(SepaMandateService);
        });

        describe('save', () => {
            it('Should call update service on save for existing entity', fakeAsync(() => {
                // GIVEN
                const entity = new SepaMandate(123);
                spyOn(service, 'update').and.returnValue(of(new HttpResponse({ body: entity })));
                comp.sepaMandate = entity;
                // WHEN
                comp.save();
                tick(); // simulate async

                // THEN
                expect(service.update).toHaveBeenCalledWith(entity);
                expect(comp.isSaving).toEqual(false);
            }));

            it('Should call create service on save for new entity', fakeAsync(() => {
                // GIVEN
                const entity = new SepaMandate();
                spyOn(service, 'create').and.returnValue(of(new HttpResponse({ body: entity })));
                comp.sepaMandate = entity;
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
