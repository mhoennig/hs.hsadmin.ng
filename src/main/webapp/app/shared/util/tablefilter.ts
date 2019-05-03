import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

export interface QueryDeclarations {
    [key: string]: string;
}
export type DynamicQueryDefinition = (name: string, value: string) => QueryDefinitions;
export interface QueryDefinitions {
    [key: string]: string | DynamicQueryDefinition;
}

/**
 * Handles filtering in data tables by converting the user input to query criteria of the JHipster REST API.
 *
 * It also does not reload during a given debounce period.
 */
export class TableFilter<T extends {}> {
    criteria: T;

    private criteriaChangedSubject = new Subject<void>();
    private criteriaChangedDebouncer: Subscription;

    constructor(private query: QueryDefinitions, private debounceMillis: number, private reload: () => void) {
        this.criteria = {} as any;
        this.criteriaChangedDebouncer = this.criteriaChangedSubject.pipe(debounceTime(debounceMillis)).subscribe(() => this.reload());
    }

    trigger($event) {
        this.debounce();
    }

    reset() {
        this.criteria = {} as any;
        this.debounce();
    }

    buildQueryCriteria(): QueryDeclarations {
        let queryCriteria: any = {} as any;
        Object.keys(this.criteria).forEach(name => {
            const value = this.criteria[name];
            if (value === '--') {
                queryCriteria[name + '.specified'] = false;
            } else if (value === '++') {
                queryCriteria[name + '.specified'] = true;
            } else {
                const queryDef = this.query[name];
                if (typeof queryDef !== 'function') {
                    queryCriteria[queryDef] = value;
                } else {
                    const additionalQueryCriteria = queryDef(name, value);
                    queryCriteria = { ...queryCriteria, ...additionalQueryCriteria };
                }
            }
        });
        return queryCriteria;
    }

    private debounce() {
        this.criteriaChangedSubject.next();
    }
}

export function queryYearAsDateRange(name: string, value: string) {
    if (value.length === 'YYYY'.length) {
        const queryCriteria: any = {} as any;
        queryCriteria[name + '.greaterOrEqualThan'] = value + '-01-01';
        queryCriteria[name + '.lessOrEqualThan'] = value + '-12-31';
        return queryCriteria;
    }
    return null;
}

export function queryEquals(name: string, value: string) {
    return { [name + '.equals']: value };
}

export function queryContains(name: string, value: string) {
    return { [name + '.contains']: value };
}
