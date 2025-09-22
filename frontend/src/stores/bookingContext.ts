import { createContext } from 'react';
import { Booking, Room } from '../types';

// State types
export interface BookingState {
    bookings: Booking[];
    rooms: Room[];
    loading: boolean;
    error: string | null;
}

// Action types
export type BookingAction =
    | { type: 'SET_LOADING' }
    | { type: 'SET_ERROR'; payload: string }
    | { type: 'SET_BOOKINGS'; payload: Booking[] }
    | { type: 'SET_ROOMS'; payload: Room[] }
    | { type: 'ADD_BOOKING'; payload: Booking }
    | { type: 'UPDATE_BOOKING'; payload: Booking }
    | { type: 'REMOVE_BOOKING'; payload: number }
    | { type: 'UPDATE_AI_DECISION'; payload: { bookingId: number; aiDecision: Booking['aiDecision'] } };

// Context interface
export interface BookingContextType {
    state: BookingState;
    loadBookings: () => Promise<void>;
    loadRooms: () => Promise<void>;
    createBooking: (bookingData: Omit<Booking, 'id' | 'status'>) => Promise<Booking>;
    handleBookingApproval: (bookingId: number, action: 'approve' | 'reject', comments?: string) => Promise<void>;
    refreshAiDecision: (bookingId: number) => Promise<void>;
}

// Initial state
export const initialState: BookingState = {
    bookings: [],
    rooms: [],
    loading: false,
    error: null,
};

// Create context
export const BookingContext = createContext<BookingContextType | undefined>(undefined);

// Reducer
export function bookingReducer(state: BookingState, action: BookingAction): BookingState {
    switch (action.type) {
        case 'SET_LOADING':
            return { ...state, loading: true, error: null };
        case 'SET_ERROR':
            return { ...state, loading: false, error: action.payload };
        case 'SET_BOOKINGS':
            return { ...state, bookings: action.payload, loading: false };
        case 'SET_ROOMS':
            return { ...state, rooms: action.payload, loading: false };
        case 'ADD_BOOKING':
            return { ...state, bookings: [...state.bookings, action.payload] };
        case 'UPDATE_BOOKING':
            return {
                ...state,
                bookings: state.bookings.map(b =>
                    b.id === action.payload.id ? action.payload : b
                ),
            };
        case 'REMOVE_BOOKING':
            return {
                ...state,
                bookings: state.bookings.filter(b => b.id !== action.payload),
            };
        case 'UPDATE_AI_DECISION':
            return {
                ...state,
                bookings: state.bookings.map(b =>
                    b.id === action.payload.bookingId
                        ? { ...b, aiDecision: action.payload.aiDecision }
                        : b
                ),
            };
        default:
            return state;
    }
}