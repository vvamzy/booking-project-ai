import React, { createContext, useContext, useReducer, useCallback } from 'react';
import { Booking, Room, BookingRequest } from '../types';
import { 
    getAllBookings, 
    getRooms, 
    bookRoom as apiBookRoom,
    updateBookingStatus,
    getAiDecision
} from '../api/apiClient';

// State types
interface BookingState {
    bookings: Booking[];
    rooms: Room[];
    loading: boolean;
    error: string | null;
}

// Action types
type BookingAction =
    | { type: 'SET_LOADING' }
    | { type: 'SET_ERROR'; payload: string }
    | { type: 'SET_BOOKINGS'; payload: Booking[] }
    | { type: 'SET_ROOMS'; payload: Room[] }
    | { type: 'ADD_BOOKING'; payload: Booking }
    | { type: 'UPDATE_BOOKING'; payload: Booking }
    | { type: 'REMOVE_BOOKING'; payload: number }
    | { type: 'UPDATE_AI_DECISION'; payload: { bookingId: number; aiDecision: Booking['aiDecision'] } };

// Initial state
const initialState: BookingState = {
    bookings: [],
    rooms: [],
    loading: false,
    error: null,
};

// Context type
interface BookingContextType {
    state: BookingState;
    loadBookings: () => Promise<void>;
    loadRooms: () => Promise<void>;
    createBooking: (bookingData: Omit<Booking, 'id' | 'status'>) => Promise<Booking>;
    handleBookingApproval: (bookingId: number, action: 'APPROVE' | 'REJECT', comments?: string) => Promise<void>;
    refreshAiDecision: (bookingId: number) => Promise<void>;
}

// Create context
const BookingContext = createContext<BookingContextType | undefined>(undefined);

// Reducer
function bookingReducer(state: BookingState, action: BookingAction): BookingState {
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

// Provider component
export function BookingProvider({ children }: { children: React.ReactNode }) {
    const [state, dispatch] = useReducer(bookingReducer, initialState);

    const loadBookings = useCallback(async () => {
        dispatch({ type: 'SET_LOADING' });
        try {
            const bookings = await getAllBookings();
            dispatch({ type: 'SET_BOOKINGS', payload: bookings });

            // Fetch AI decisions for pending bookings
            const pendingStatuses = ['PENDING', 'PENDING_APPROVAL'];
            const pendingBookings = bookings.filter(b => pendingStatuses.includes(String(b.status)));
            await Promise.all(
                pendingBookings.map(async (booking) => {
                    try {
                        const aiDecision = await getAiDecision(booking.id);
                        if (aiDecision) {
                            dispatch({
                                type: 'UPDATE_AI_DECISION',
                                payload: { bookingId: booking.id, aiDecision },
                            });
                        }
                    } catch (err) {
                        console.warn(`Could not fetch AI decision for booking ${booking.id}:`, err);
                    }
                })
            );
        } catch (error) {
            dispatch({ 
                type: 'SET_ERROR', 
                payload: error instanceof Error ? error.message : 'Failed to load bookings' 
            });
        }
    }, []);

    const loadRooms = useCallback(async () => {
        dispatch({ type: 'SET_LOADING' });
        try {
            const rooms = await getRooms();
            dispatch({ type: 'SET_ROOMS', payload: rooms });
        } catch (error) {
            dispatch({ 
                type: 'SET_ERROR', 
                payload: error instanceof Error ? error.message : 'Failed to load rooms' 
            });
        }
    }, []);

    const createBooking = useCallback(async (bookingData: Omit<BookingRequest, 'status'>) => {
        try {
            const request: BookingRequest = {
                ...bookingData,
                status: 'PENDING'
            };
            const newBooking = await apiBookRoom(request);
            dispatch({ type: 'ADD_BOOKING', payload: newBooking });
            return newBooking;
        } catch (error) {
            const message = error instanceof Error ? error.message : 'Failed to create booking';
            dispatch({ type: 'SET_ERROR', payload: message });
            throw new Error(message);
        }
    }, []);

    const handleBookingApproval = useCallback(async (bookingId: number, action: 'APPROVE' | 'REJECT', comments?: string) => {
        try {
            const actionLower = action === 'APPROVE' ? 'approve' : 'reject';
            await updateBookingStatus(bookingId, actionLower, comments);
            dispatch({ type: 'REMOVE_BOOKING', payload: bookingId });
        } catch (error) {
            const message = error instanceof Error ? error.message : `Failed to ${action.toLowerCase()} booking`;
            dispatch({ type: 'SET_ERROR', payload: message });
            throw new Error(message);
        }
    }, []);

    const refreshAiDecision = useCallback(async (bookingId: number) => {
        try {
            const aiDecision = await getAiDecision(bookingId);
            if (aiDecision) {
                dispatch({
                    type: 'UPDATE_AI_DECISION',
                    payload: { bookingId, aiDecision },
                });
            }
        } catch (error) {
            console.warn(`Could not refresh AI decision for booking ${bookingId}:`, error);
        }
    }, []);

    const value: BookingContextType = {
        state,
        loadBookings,
        loadRooms,
        createBooking,
        handleBookingApproval,
        refreshAiDecision,
    };

    return <BookingContext.Provider value={value}>{children}</BookingContext.Provider>;
}

// Custom hook to use the booking context
export function useBooking(): BookingContextType {
    const context = useContext(BookingContext);
    if (context === undefined) {
        throw new Error('useBooking must be used within a BookingProvider');
    }
    return context;
}