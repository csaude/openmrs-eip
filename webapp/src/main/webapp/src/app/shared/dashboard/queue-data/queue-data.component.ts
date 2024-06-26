import {Component, Input, OnDestroy, OnInit} from "@angular/core";
import {Subscription} from 'rxjs';
import {select, Store} from "@ngrx/store";
import {QueueData} from "./queue-data";
import {
	FetchQueueCategories,
	FetchQueueCategoryCount,
	FetchQueueCount,
	QueueCategoriesReceived,
	QueueCountReceived
} from "../state/dashboard.actions";
import {DashboardService} from "../dashboard.service";
import {
	GET_CONFLICT_CATEGORIES,
	GET_CONFLICT_CATEGORY_COUNTS,
	GET_CONFLICT_COUNT,
	GET_ERROR_CATEGORIES,
	GET_ERROR_CATEGORY_COUNTS,
	GET_ERROR_COUNT,
	GET_EVENT_CATEGORIES,
	GET_EVENT_CATEGORY_COUNTS,
	GET_EVENT_COUNT,
	GET_SENDER_ERROR_CATEGORIES,
	GET_SENDER_ERROR_CATEGORY_COUNTS,
	GET_SENDER_ERROR_COUNT,
	GET_SENDER_SYNC_CATEGORIES,
	GET_SENDER_SYNC_CATEGORY_COUNTS,
	GET_SENDER_SYNC_COUNT,
	GET_SYNC_CATEGORIES,
	GET_SYNC_CATEGORY_COUNTS,
	GET_SYNC_COUNT,
	GET_SYNCED_CATEGORIES,
	GET_SYNCED_CATEGORY_COUNTS,
	GET_SYNCED_COUNT
} from "../state/dashboard.reducer";
import {Selector} from "@ngrx/store/src/models";
import {SyncOperation} from "../../sync-operation.enum";
import {SyncMode} from "../../sync-mode.enum";
import {GET_PROPS} from "../../../state/app.reducer";

@Component({
	selector: 'queue-data',
	templateUrl: './queue-data.component.html'
})
export class QueueDataComponent implements OnInit, OnDestroy {

	readonly SYNC_OPS = Object.values(SyncOperation);

	readonly receiverQueueNames = ['sync', 'synced', 'error', 'conflict'];

	readonly queueAndCountSelectorMap = new Map<string, Selector<object, number | null | undefined>>([
		['sync', GET_SYNC_COUNT],
		['synced', GET_SYNCED_COUNT],
		['error', GET_ERROR_COUNT],
		['conflict', GET_CONFLICT_COUNT],
		['event', GET_EVENT_COUNT],
		['sender-sync', GET_SENDER_SYNC_COUNT],
		['sender-error', GET_SENDER_ERROR_COUNT]
	]);

	readonly queueAndCategoriesSelectorMap = new Map<string, Selector<object, string[] | undefined>>([
		['sync', GET_SYNC_CATEGORIES],
		['synced', GET_SYNCED_CATEGORIES],
		['error', GET_ERROR_CATEGORIES],
		['conflict', GET_CONFLICT_CATEGORIES],
		['event', GET_EVENT_CATEGORIES],
		['sender-sync', GET_SENDER_SYNC_CATEGORIES],
		['sender-error', GET_SENDER_ERROR_CATEGORIES]
	]);

	readonly queueAndCatCountSelectorMap = new Map<string, Selector<object, Map<string, Map<SyncOperation, number>> | undefined>>([
		['sync', GET_SYNC_CATEGORY_COUNTS],
		['synced', GET_SYNCED_CATEGORY_COUNTS],
		['error', GET_ERROR_CATEGORY_COUNTS],
		['conflict', GET_CONFLICT_CATEGORY_COUNTS],
		['event', GET_EVENT_CATEGORY_COUNTS],
		['sender-sync', GET_SENDER_SYNC_CATEGORY_COUNTS],
		['sender-error', GET_SENDER_ERROR_CATEGORY_COUNTS]
	]);

	readonly data = new QueueData();

	breakdownLabel?: string;

	appSyncMode?: SyncMode;

	@Input()
	parentSyncMode?: SyncMode;

	@Input()
	queueName: string = '';

	countSelector: Selector<object, number | null | undefined> | undefined;

	categorySelector: Selector<object, string[] | undefined> | undefined;

	categoryAndCountsSelector: Selector<object, Map<string, Map<SyncOperation, number>> | undefined> | undefined;

	timeoutId?: number;

	receivedCategoryCounts: number = 0;

	propsLoadedSubscription?: Subscription;

	countSubscription?: Subscription;

	categoriesSubscription?: Subscription;

	categoryCountSubscription?: Subscription;

	constructor(private service: DashboardService, private store: Store) {
	}

	ngOnInit(): void {
		this.propsLoadedSubscription = this.store.pipe(select(GET_PROPS)).subscribe(props => {
			if (props.syncMode) {
				this.appSyncMode = props.syncMode;

				if (this.canLoad()) {
					if (this.receiverQueueNames.indexOf(this.queueName) > -1) {
						this.breakdownLabel = $localize`:@@common-entity-breakdown:Entity Breakdown`;
					} else {
						this.breakdownLabel = $localize`:@@common-db-table-breakdown:Database Table Breakdown`;
					}

					this.countSelector = this.queueAndCountSelectorMap.get(this.queueName);
					if (this.countSelector) {
						this.countSubscription = this.store.pipe(select(this.countSelector)).subscribe(count => {
							this.onCountChange(count);
						});
					}

					this.categorySelector = this.queueAndCategoriesSelectorMap.get(this.queueName);
					if (this.categorySelector) {
						this.categoriesSubscription = this.store.pipe(select(this.categorySelector)).subscribe(categories => {
							this.onCategoriesChange(categories);
						});
					}

					this.categoryAndCountsSelector = this.queueAndCatCountSelectorMap.get(this.queueName);
					if (this.categoryAndCountsSelector) {
						this.categoryCountSubscription = this.store.pipe(select(this.categoryAndCountsSelector)).subscribe(catAndCounts => {
							this.onCategoryCountsChange(catAndCounts);
						});
					}

					this.scheduleLoadData(0);
				}
			}
		});
	}

	scheduleReload(): void {
		this.scheduleLoadData(30000);
	}

	scheduleLoadData(delay: number): void {
		this.timeoutId = setTimeout(() => {
			if (delay > 0) {
				this.reset();
			}

			this.getCount();
		}, delay);
	}

	reset(): void {
		this.receivedCategoryCounts = 0;
		this.store.dispatch(new QueueCountReceived(null, this.queueName));
	}

	updateCount(count: number): void {
		this.store.dispatch(new QueueCountReceived(count, this.queueName));
	}

	clearCategories(): void {
		this.store.dispatch(new QueueCategoriesReceived([], this.queueName));
	}

	getCount(): void {
		this.store.dispatch(new FetchQueueCount(this.queueName));
	}

	getCategories(): void {
		this.store.dispatch(new FetchQueueCategories(this.queueName));
	}

	getCategoryCounts(): void {
		this.data.categories?.forEach(c => {
			this.SYNC_OPS.forEach(o => {
				this.store.dispatch(new FetchQueueCategoryCount(this.queueName, c, o));
			});
		});
	}

	canLoad(): boolean {
		if (this.appSyncMode == SyncMode.RECEIVER && this.receiverQueueNames.indexOf(this.queueName) > -1) {
			return true;
		} else if (this.appSyncMode == SyncMode.SENDER && this.receiverQueueNames.indexOf(this.queueName) < 0) {
			return true;
		}

		return false;
	}

	onCountChange(count?: number | null): void {
		//Ignore because this is the first event when initializing the component state.
		if (count !== undefined) {
			//Don't update the UI yet because this is a count reset event
			if (count !== null) {
				this.data.count = count;
				if (this.data.count > 0) {
					this.getCategories();
				} else {
					this.clearCategories();
					this.scheduleReload();
				}
			}
		}
	}

	onCategoriesChange(categories?: string[]): void {
		//Ignore because this is the first value when initializing the component state.
		if (categories) {
			this.data.categories = categories;
			if (this.data.categories.length > 0) {
				this.getCategoryCounts();
			} else {
				this.updateCount(0);
				this.scheduleReload();
			}
		}
	}

	onCategoryCountsChange(catAndCounts?: Map<string, Map<SyncOperation, number>>): void {
		if (catAndCounts) {
			this.data.categoryAndCounts = catAndCounts;
			this.receivedCategoryCounts++;
			//We're done fetching all the queue data
			if (this.data.categories && this.receivedCategoryCounts == (this.data.categories.length * this.SYNC_OPS.length)) {
				//Update queue count to match the sum of all the counts by type and operation
				let effectiveCount: number = 0;
				this.data.categoryAndCounts?.forEach((opAndCount: Map<SyncOperation, number>, category: string) => {
					opAndCount.forEach((count: number | null, op: SyncOperation) => {
						if (count) {
							effectiveCount += count;
						}
					});
				});

				this.updateCount(effectiveCount);
				this.scheduleReload();
			}
		}
	}

	ngOnDestroy(): void {
		this.stopSubscriptions();
	}

	stopSubscriptions(): void {
		clearTimeout(this.timeoutId);
		this.propsLoadedSubscription?.unsubscribe();
		this.countSubscription?.unsubscribe();
		this.categoriesSubscription?.unsubscribe();
		this.categoryCountSubscription?.unsubscribe();
	}

}
