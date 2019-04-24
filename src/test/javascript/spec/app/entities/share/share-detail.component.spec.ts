/* tslint:disable max-line-length */
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { of } from 'rxjs';

import { HsadminNgTestModule } from '../../../test.module';
import { ShareDetailComponent } from 'app/entities/share/share-detail.component';
import { Share } from 'app/shared/model/share.model';

describe('Component Tests', () => {
    describe('Share Management Detail Component', () => {
        let comp: ShareDetailComponent;
        let fixture: ComponentFixture<ShareDetailComponent>;
        const route = ({ data: of({ share: new Share(123) }) } as any) as ActivatedRoute;

        beforeEach(() => {
            TestBed.configureTestingModule({
                imports: [HsadminNgTestModule],
                declarations: [ShareDetailComponent],
                providers: [{ provide: ActivatedRoute, useValue: route }]
            })
                .overrideTemplate(ShareDetailComponent, '')
                .compileComponents();
            fixture = TestBed.createComponent(ShareDetailComponent);
            comp = fixture.componentInstance;
        });

        describe('OnInit', () => {
            it('Should call load all on init', () => {
                // GIVEN

                // WHEN
                comp.ngOnInit();

                // THEN
                expect(comp.share).toEqual(jasmine.objectContaining({ id: 123 }));
            });
        });
    });
});
