import { LinebreaksPipe } from 'app/shared/util/linebreaks-pipe';

/* To run these tests in IntelliJ IDEA, you need a run configuration with
    Configuration File:
        ~/Projekte/Hostsharing/hsadmin-ng/src/test/javascript/jest.conf.js
    and a Node Interpreter, e.g. if installed with nvm, this could be:
        ~/.nvm/versions/node/v10.15.3/bin/node
 */
describe('LinebreaksPipe Tests', () => {
    describe('LinebreaksPipe', () => {
        let pipe: LinebreaksPipe;

        beforeEach(() => {
            pipe = new LinebreaksPipe();
        });

        it('converts null to null', () => {
            expect(pipe.transform(null)).toBe(null);
        });

        it('converts empty string to empty string', () => {
            expect(pipe.transform('')).toBe('');
        });

        it('converts string not containing line breaks to identical string', () => {
            expect(pipe.transform('no linebreak here')).toBe('no linebreak here');
        });

        it('converts string containing line breaks to string containing <br/> by default', () => {
            expect(pipe.transform('some\nlinebreaks\nhere')).toBe('some<br/>linebreaks<br/>here');
        });

        it('converts string containing line breaks string containing specified replacement', () => {
            expect(pipe.transform('some\nlinebreaks\nhere', ' | ')).toBe('some | linebreaks | here');
        });
    });
});
