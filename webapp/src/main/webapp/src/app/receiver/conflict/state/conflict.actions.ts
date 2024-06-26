import {Action} from "@ngrx/store";
import {ConflictCountAndItems} from "../confict-count-and-items";
import {ConflictTaskStatus} from "../conflict-task-status";
import {Diff} from "../diff";

export enum ConflictActionType {
	CONFLICTS_LOADED = 'CONFLICTS_LOADED',
	VIEW_DIFF = 'VIEW_DIFF',
	VERIFY_TASK_STATUS_UPDATED = 'VERIFY_TASK_STATUS_UPDATED',
	RESOLVER_TASK_STATUS_UPDATED = 'RESOLVER_TASK_STATUS_UPDATED'
}

export class ConflictsLoaded implements Action {

	readonly type = ConflictActionType.CONFLICTS_LOADED;

	constructor(public countAndItems?: ConflictCountAndItems) {
	}

}

export class ViewDiff implements Action {

	readonly type = ConflictActionType.VIEW_DIFF;

	constructor(public diff?: Diff) {
	}

}

export class VerifyTaskStatusUpdated implements Action {

	readonly type = ConflictActionType.VERIFY_TASK_STATUS_UPDATED;

	constructor(public verifyTaskStatus?: ConflictTaskStatus) {
	}

}

export class ResolverTaskStatusUpdated implements Action {

	readonly type = ConflictActionType.RESOLVER_TASK_STATUS_UPDATED;

	constructor(public resolverTaskStatus?: ConflictTaskStatus) {
	}

}

export type ConflictAction = ConflictsLoaded | ViewDiff | VerifyTaskStatusUpdated | ResolverTaskStatusUpdated;
