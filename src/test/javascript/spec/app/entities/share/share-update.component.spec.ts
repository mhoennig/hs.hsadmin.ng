/* tslint:disable max-line-length */
import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpResponse } from '@angular/common/http';
import { Observable, of } from 'rxjs';

import { HsadminNgTestModule } from '../../../test.module';
import { ShareUpdateComponent } from 'app/entities/share/share-update.component';
import { ShareService } from 'app/entities/share/share.service';
import { Share } from 'app/shared/model/share.model';

describe('Component Tests', () => {
    describe('Share Management Update Component', () => {
        let comp: ShareUpdateComponent;
        let fixture: ComponentFixture<ShareUpdateComponent>;
        let service: ShareService;

        beforeEach(() => {
            TestBed.configureTestingModule({
                imports: [HsadminNgTestModule],
                declarations: [ShareUpdateComponent]
            })
                .overrideTemplate(ShareUpdateComponent, '')
                .compileComponents();

            fixture = TestBed.createComponent(ShareUpdateComponent);
            comp = fixture.componentInstance;
            service = fixture.debugElement.injector.get(ShareService);
        });

        describe('save', () => {
            it('Should call update service on save for existing entity', fakeAsync(() => {
                // GIVEN
                const entity = new Share(123);
                spyOn(service, 'update').and.returnValue(of(new HttpResponse({ body: entity })));
                comp.share = entity;
                // WHEN
                comp.save();
                tick(); // simulate async

                // THEN
                expect(service.update).toHaveBeenCalledWith(entity);
                expect(comp.isSaving).toEqual(false);
            }));

            it('Should call create service on save for new entity', fakeAsync(() => {
                // GIVEN
                const entity = new Share();
                spyOn(service, 'create').and.returnValue(of(new HttpResponse({ body: entity })));
                comp.share = entity;
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
