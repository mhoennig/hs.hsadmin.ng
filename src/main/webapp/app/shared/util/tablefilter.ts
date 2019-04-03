import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';

/**
 * Handles filtering in data tables by converting the user input to query criteria of the JHipster REST API.
 *
 * It also does not reload during a given debounce period.
 */
export class TableFilter<T extends {}> {
    criteria: T;

    private criteriaChangedSubject = new Subject<void>();
    private criteriaChangedDebouncer: Subscription;

    constructor(private query: T, private debounceMillis: number, private reload: () => void) {
        this.criteria = {} as any;
        this.criteriaChangedDebouncer = this.criteriaChangedSubject.pipe(debounceTime(debounceMillis)).subscribe(() => this.reload());
    }

    trigger() {
        this.debounce();
    }

    reset() {
        this.criteria = {} as any;
        this.debounce();
    }

    buildQueryCriteria() {
        let queryCriteria: T = {} as any;
        Object.keys(this.criteria).forEach(e => {
            queryCriteria[e + '.' + this.query[e]] = this.criteria[e];
        });
        return queryCriteria;
    }

    private debounce() {
        this.criteriaChangedSubject.next();
    }
}
