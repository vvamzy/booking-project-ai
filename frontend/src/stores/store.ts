import { createStore, Action } from 'redux';
import { Booking, User, Approval } from '../types';

// Action types
export const ADD_BOOKING = 'ADD_BOOKING';
export const REMOVE_BOOKING = 'REMOVE_BOOKING';
export const ADD_APPROVAL = 'ADD_APPROVAL';
export const REMOVE_APPROVAL = 'REMOVE_APPROVAL';
export const SET_USER = 'SET_USER';

// Action interfaces
interface AddBookingAction extends Action {
    type: typeof ADD_BOOKING;
    payload: Booking;
}

interface RemoveBookingAction extends Action {
    type: typeof REMOVE_BOOKING;
    payload: number; // booking id
}

interface AddApprovalAction extends Action {
    type: typeof ADD_APPROVAL;
    payload: Approval;
}

interface RemoveApprovalAction extends Action {
    type: typeof REMOVE_APPROVAL;
    payload: number; // approval id
}

interface SetUserAction extends Action {
    type: typeof SET_USER;
    payload: User | null;
}

// Union type for all actions
type AppAction = 
    | AddBookingAction 
    | RemoveBookingAction 
    | AddApprovalAction 
    | RemoveApprovalAction 
    | SetUserAction;

// State interface
interface AppState {
    bookings: Booking[];
    approvals: Approval[];
    user: User | null;
}

const initialState: AppState = {
    bookings: [],
    approvals: [],
    user: null,
};

const rootReducer = (state: AppState = initialState, action: AppAction): AppState => {
    switch (action.type) {
        case ADD_BOOKING:
            return {
                ...state,
                bookings: [...state.bookings, action.payload],
            };
        case REMOVE_BOOKING:
            return {
                ...state,
                bookings: state.bookings.filter(booking => booking.id !== action.payload),
            };
        case ADD_APPROVAL:
            return {
                ...state,
                approvals: [...state.approvals, action.payload],
            };
        case REMOVE_APPROVAL:
            return {
                ...state,
                approvals: state.approvals.filter(approval => approval.id !== action.payload),
            };
        case SET_USER:
            return {
                ...state,
                user: action.payload,
            };
        default:
            return state;
    }
};

const store = createStore(rootReducer);

export default store;