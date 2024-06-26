import {createFeatureSelector, createSelector} from "@ngrx/store";
import {SenderError} from "../sender-error";
import {SenderErrorAction, SenderErrorActionType} from "./error.actions";
import {SenderErrorCountAndItems} from "../sender-error-count-and-items";

export interface SenderErrorState {
	countAndItems: SenderErrorCountAndItems;
	errorToView?: SenderError;
}

const GET_SENDER_ERRORS_FEATURE_STATE = createFeatureSelector<SenderErrorState>('senderErrorQueue');

export const GET_SENDER_ERRORS = createSelector(
	GET_SENDER_ERRORS_FEATURE_STATE,
	state => state.countAndItems
);

export const SENDER_ERROR_TO_VIEW = createSelector(
	GET_SENDER_ERRORS_FEATURE_STATE,
	state => state.errorToView
);

const initialSenderState: SenderErrorState = {
	countAndItems: new SenderErrorCountAndItems()
};

export function senderErrorReducer(state = initialSenderState, action: SenderErrorAction) {

	switch (action.type) {

		case SenderErrorActionType.SENDER_ERRORS_LOADED:
			return {
				...state,
				countAndItems: action.countAndItems
			};

		case SenderErrorActionType.VIEW_SENDER_ERROR:
			return {
				...state,
				errorToView: action.error
			};

		default:
			return state;
	}

}
