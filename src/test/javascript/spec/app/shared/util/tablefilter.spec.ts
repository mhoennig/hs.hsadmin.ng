import { queryContains, queryEquals, queryYearAsDateRange, TableFilter } from 'app/shared/util/tablefilter';

/* To run these tests in IntelliJ IDEA, you need a run configuration with
    Configuration File:
        ~/Projekte/Hostsharing/hsadmin-ng/src/test/javascript/jest.conf.js
    and a Node Interpreter, e.g. if installed with nvm, this could be:
        ~/.nvm/versions/node/v10.15.3/bin/node
 */
describe('TableFilter Tests', () => {
    describe('TableFilter', () => {
        const TEST_DEBOUNCE_MILLIS = 100;

        let filter: TableFilter<{ name?: string; number?: string; date?: string }>;
        let asynchronously: () => void;

        beforeEach(() => {
            filter = new TableFilter(
                {
                    name: queryContains,
                    number: queryEquals,
                    date: queryYearAsDateRange
                },
                TEST_DEBOUNCE_MILLIS,
                () => {
                    asynchronously();
                }
            );
        });

        it('trigger() asynchronously calls the reload-handler', done => {
            // given
            filter.criteria.name = 'Test Filter Value';

            // when
            filter.trigger({ target: { valid: true } });
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
            filter.trigger({ target: { valid: true } });
            let triggerStartedAtMillis = null;
            setTimeout(() => {
                filter.trigger({ target: { valid: true } });
                triggerStartedAtMillis = Date.now();
            }, 50);

            // then
            asynchronously = () => {
                expect(triggerStartedAtMillis).not.toBeNull();
                expect(Date.now()).toBeGreaterThan(triggerStartedAtMillis);
                done();
            };
        });

        it('when filter "name" is set, buildQueryCriteria() returns a name.contains query', () => {
            // given
            filter.criteria.name = 'test value';

            // when
            const actual = filter.buildQueryCriteria();

            // then
            expect(filter.buildQueryCriteria()).toEqual({ 'name.contains': 'test value' });
        });

        it('when filter "number" is set, buildQueryCriteria() returns a number.equals query', () => {
            // given
            filter.criteria.number = '-42';

            // when
            const actual = filter.buildQueryCriteria();

            // then
            expect(filter.buildQueryCriteria()).toEqual({ 'number.equals': '-42' });
        });

        it('when filter "date" is set to "2019", buildQueryCriteria() returns a date range query', () => {
            // given
            filter.criteria.date = '2019';

            // when
            const actual = filter.buildQueryCriteria();

            // then
            expect(filter.buildQueryCriteria()).toEqual({ 'date.greaterOrEqualThan': '2019-01-01', 'date.lessOrEqualThan': '2019-12-31' });
        });

        it('queryYearAsDateRange() returns null if year is not 4-digit', () => {
            expect(queryYearAsDateRange('date', '201')).toBeNull();
            expect(queryYearAsDateRange('date', '20191')).toBeNull();
        });

        it('reset() clears criteria and calls reload-handler, considering debounce period', done => {
            // given
            filter.criteria.name = 'Test Filter Value';

            // when
            filter.trigger({ target: { valid: true } });
            let triggerStartedAtMillis = null;
            setTimeout(() => {
                filter.reset();
                triggerStartedAtMillis = Date.now();
            }, TEST_DEBOUNCE_MILLIS / 2);

            // then
            asynchronously = () => {
                expect(triggerStartedAtMillis).not.toBeNull();
                expect(Date.now()).toBeGreaterThan(triggerStartedAtMillis);
                done();
            };
        });
    });
});
