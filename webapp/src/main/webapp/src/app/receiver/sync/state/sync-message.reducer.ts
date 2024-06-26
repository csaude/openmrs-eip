import {createFeatureSelector, createSelector} from "@ngrx/store";
import {ReceiverSyncMessage} from "../receiver-sync-message";
import {SyncMessageAction, SyncMessageActionType} from "./sync-message.actions";
import {ViewInfo} from "../../shared/view-info";

export interface SyncMessageState {
	totalCount?: number;

	viewInfo?: ViewInfo;

	syncItems?: ReceiverSyncMessage[];

	msgToView?: ReceiverSyncMessage;

	siteCountMap?: Map<string, number>;
}

const GET_MSG_FEATURE_STATE = createFeatureSelector<SyncMessageState>('syncMsgQueue');

export const GET_SYNC_MSGS = createSelector(
	GET_MSG_FEATURE_STATE,
	state => state.syncItems
);

export const MSG_TO_VIEW = createSelector(
	GET_MSG_FEATURE_STATE,
	state => state.msgToView
);

export const GET_SYNC_MSG_TOTAL_COUNT = createSelector(
	GET_MSG_FEATURE_STATE,
	state => state.totalCount
);

export const GET_SYNC_MSG_VIEW = createSelector(
	GET_MSG_FEATURE_STATE,
	state => state.viewInfo
);

export const GET_SYNC_MSG_GRP_PROP_COUNT_MAP = createSelector(
	GET_MSG_FEATURE_STATE,
	state => state.siteCountMap
);

export function syncMessageReducer(state = {}, action: SyncMessageAction) {

	switch (action.type) {

		case SyncMessageActionType.SYNC_MSGS_LOADED:
			return {
				...state,
				totalCount: action.countAndItems?.count,
				syncItems: action.countAndItems?.items
			};

		case SyncMessageActionType.VIEW_SYNC_MSG:
			return {
				...state,
				msgToView: action.message
			};

		case SyncMessageActionType.CHANGE_SYNC_MSG_VIEW:
			return {
				...state,
				totalCount: undefined,
				viewInfo: action.viewInfo
			};

		case SyncMessageActionType.GROUPED_SYNC_MSGS_LOADED:
			return {
				...state,
				totalCount: action.countAndGroupedItems?.count,
				siteCountMap: action.countAndGroupedItems?.items
			};

		default:
			return state;
	}

}
