import { NgModule } from '@angular/core';

import { FindLanguageFromKeyPipe, HsadminNgSharedLibsModule, JhiAlertComponent, JhiAlertErrorComponent } from './';
import { LinebreaksPipe } from 'app/shared/util/linebreaks-pipe';

@NgModule({
    imports: [HsadminNgSharedLibsModule],
    declarations: [FindLanguageFromKeyPipe, LinebreaksPipe, JhiAlertComponent, JhiAlertErrorComponent],
    exports: [HsadminNgSharedLibsModule, FindLanguageFromKeyPipe, LinebreaksPipe, JhiAlertComponent, JhiAlertErrorComponent]
})
export class HsadminNgSharedCommonModule {}
