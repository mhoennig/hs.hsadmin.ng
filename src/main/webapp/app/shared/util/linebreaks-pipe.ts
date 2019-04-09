import { Pipe, PipeTransform } from '@angular/core';

@Pipe({ name: 'linebreaks' })
export class LinebreaksPipe implements PipeTransform {
    transform(text: string, as: string = '<br/>'): string {
        if (text == null) {
            return null;
        }
        return text.replace(/\n/g, as);
    }
}
