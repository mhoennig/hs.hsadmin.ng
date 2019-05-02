import { TableFilter } from 'app/shared/util/tablefilter';

/* To run these tests in IntelliJ IDEA, you need a run configuration with
    Configuration File:
        ~/Projekte/Hostsharing/hsadmin-ng/src/test/javascript/jest.conf.js
    and a Node Interpreter, e.g. if installed with nvm, this could be:
        ~/.nvm/versions/node/v10.15.3/bin/node
 */
describe('TableFilter Tests', () => {
    describe('TableFilter', () => {
        let filter: TableFilter<{ name: string; number: string }>;
        let asynchronously: () => void;

        beforeEach(() => {
            filter = new TableFilter({ name: 'contains', number: 'equals' }, 100, () => {
                asynchronously();
            });
        });

        it('trigger() asynchronously calls the reload-handler', done => {
            // given
            filter.criteria.name = 'Test Filter Value';

            // when
            filter.trigger();
            const triggerStartedAtMillis = Date.now();

            // then
            asynchronously = () => {
                expect(Date.now()).toBeGreaterThan(triggerStartedAtMillis);
                done();
            };
        });

        it('if trigger() is called multiple times during debounce, debounce period ands after last trigger()', done => {
            // given
            filter.criteria.name = 'Test Filter Value';

            // when
            filter.trigger();
            let triggerStartedAtMillis = null;
            setTimeout(() => {
                filter.trigger();
                triggerStartedAtMillis = Date.now();
            }, 50);

            // then
            asynchronously = () => {
                expect(triggerStartedAtMillis).not.toBeNull();
                expect(Date.now()).toBeGreaterThan(triggerStartedAtMillis);
                done();
            };
        });

        it('when filter "name" is set to "test value", buildQueryCriteria() returns { "name.contains": "test value" }', () => {
            // given
            filter.criteria.name = 'test value';

            // when
            const actual = filter.buildQueryCriteria();

            // then
            expect(filter.buildQueryCriteria()).toEqual({ 'name.contains': 'test value' });
        });

        it('reset() clears criteria and calls reload-handler, considering debounce period', done => {
            // given
            filter.criteria.name = 'Test Filter Value';

            // when
            filter.trigger();
            let triggerStartedAtMillis = null;
            setTimeout(() => {
                filter.reset();
                triggerStartedAtMillis = Date.now();
            }, 50);

            // then
            asynchronously = () => {
                expect(triggerStartedAtMillis).not.toBeNull();
                expect(Date.now()).toBeGreaterThan(triggerStartedAtMillis);
                done();
            };
        });
    });
});
