import axios, { AxiosError } from 'axios';
import { Room, Booking, BookingRequest, Approval } from '../types';

const BASE_URL = process.env.REACT_APP_API_URL || '';
const API_BASE_URL = `${BASE_URL}/api`;
const API_TIMEOUT = parseInt(process.env.REACT_APP_API_TIMEOUT || '30000', 10);
const MAX_RETRIES = parseInt(process.env.REACT_APP_MAX_RETRIES || '3', 10);
const RETRY_DELAY = parseInt(process.env.REACT_APP_RETRY_DELAY || '1000', 10);

type ApiErrorData = {
    status: number;
    data?: any;
};

class ApiError extends Error {
    status: number;
     data: any | undefined;

    constructor(status: number, message: string, data?: any) {
        super(message);
        this.name = 'ApiError';
        this.status = status;
        this.data = data;
        Object.setPrototypeOf(this, ApiError.prototype);
    }
}

// Ensure axios sends cookies for session-based auth
axios.defaults.withCredentials = true;

const apiClient = axios.create({
    baseURL: API_BASE_URL,
    timeout: API_TIMEOUT,
    headers: {
        'Content-Type': 'application/json',
    },
    withCredentials: true,
});

// Request interceptor for handling retries
apiClient.interceptors.response.use(
    (response) => response,
    async (error: AxiosError) => {
       const config: any = error.config || {};
        config.retries = config.retries || 0;

        if (config.retries < MAX_RETRIES && (error.response?.status || 500) >= 500) {
            config.retries += 1;
            await new Promise(resolve => setTimeout(resolve, RETRY_DELAY));
            return apiClient(config);
        }


        const status = error.response?.status || 500;
        const message = error.response?.data?.message || error.message;
        const data = error.response?.data;

        throw new ApiError(status, message, data);
    }
);

// Function to get all rooms
export const getRooms = async (): Promise<Room[]> => {
    try {
        const response = await axios.get(`${BASE_URL}/api/rooms`);
        return response.data;
    } catch (error) {
        if (error instanceof ApiError) throw error;
        throw new ApiError(500, 'Failed to fetch rooms');
    }
};

// Function to get room by id
export const getRoom = async (roomId: number): Promise<Room> => {
    try {
        const response = await axios.get(`${BASE_URL}/api/rooms/${roomId}`);
        return response.data;
    } catch (error) {
        if (error instanceof ApiError) throw error;
        throw new ApiError(500, 'Failed to fetch room');
    }
};

// Function to book a room
export const bookRoom = async (bookingData: BookingRequest): Promise<Booking> => {
    try {
        // Remove any client-supplied userId to avoid impersonation; backend will infer from session
        const payload = { ...bookingData };
        delete payload.userId;
        payload.priority = bookingData.priority || 3;
        payload.notes = bookingData.notes || '';
        payload.attendeesCount = bookingData.attendeesCount;
        payload.requiredFacilities = bookingData.requiredFacilities;

        const response = await axios.post(`${BASE_URL}/api/bookings`, payload);
        return response.data;
    } catch (error) {
        if (error instanceof ApiError) throw error;
        throw new ApiError(500, 'Failed to create booking');
    }
};

// Function to get all bookings
export const getAllBookings = async (): Promise<Booking[]> => {
    try {
        const response = await axios.get(`${BASE_URL}/api/bookings`);
        return response.data;
    } catch (error) {
        if (error instanceof ApiError) throw error;
        throw new ApiError(500, 'Failed to fetch bookings');
    }
};

// Function to get pending approvals
export const getPendingApprovals = async (): Promise<Booking[]> => {
    try {
        const response = await axios.get(`${BASE_URL}/api/bookings/pending`);
        return response.data;
    } catch (error) {
        if (error instanceof ApiError) throw error;
        throw new ApiError(500, 'Failed to fetch pending approvals');
    }
};

// Function to handle booking approval/rejection
export const updateBookingStatus = async (
    bookingId: number,
    action: 'approve' | 'reject',
    comments?: string
): Promise<Approval> => {
    try {
        const response = await axios.post(`${BASE_URL}/api/bookings/${bookingId}/${action}`, { comments });
        return response.data;
    } catch (error) {
        if (error instanceof ApiError) throw error;
        throw new ApiError(500, `Failed to ${action} booking`);
    }
};

// cancel booking
export const cancelBooking = async (bookingId: number, cancelledBy = 'user', reason?: string): Promise<void> => {
    try {
        await axios.put(`${BASE_URL}/api/bookings/${bookingId}/cancel`, null, { params: { cancelledBy, reason } });
    } catch (error) {
        if (error instanceof ApiError) throw error;
        throw new ApiError(500, 'Failed to cancel booking');
    }
};

// Function to get AI decision for a booking
export const getAiDecision = async (bookingId: number): Promise<Booking['aiDecision']> => {
    try {
        const response = await axios.get(`${BASE_URL}/api/bookings/${bookingId}/ai-decision`);
        return response.data;
    } catch (error) {
        if (error instanceof ApiError) throw error;
        throw new ApiError(500, 'Failed to fetch AI decision');
    }
};

// Validate a booking payload using AI/rules (without creating)
export const validateBooking = async (bookingData: BookingRequest): Promise<any> => {
    try {
        const payload = { ...bookingData };
        delete payload.userId;
        const response = await axios.post(`${BASE_URL}/api/bookings/validate`, payload);
        return response.data;
    } catch (error) {
        if (error instanceof ApiError) throw error;
        throw new ApiError(500, 'Failed to validate booking');
    }
};

// Function to request a new AI analysis
export const requestAiAnalysis = async (bookingId: number): Promise<Booking['aiDecision']> => {
    try {
        const response = await axios.post(`${BASE_URL}/api/bookings/${bookingId}/analyze`);
        return response.data;
    } catch (error) {
        if (error instanceof ApiError) throw error;
        throw new ApiError(500, 'Failed to analyze booking');
    }
};

// Function to update a booking
export const updateBooking = async (
    bookingId: number,
    bookingData: Partial<BookingRequest>
): Promise<Booking> => {
    try {
        const response = await axios.put(`${BASE_URL}/api/bookings/${bookingId}`, {
            ...bookingData,
            status: bookingData.status || 'PENDING',
            priority: bookingData.priority || 3,
            notes: bookingData.notes || '',
            attendeesCount: bookingData.attendeesCount,
            requiredFacilities: bookingData.requiredFacilities
        });
        return response.data;
    } catch (error) {
        if (error instanceof ApiError) throw error;
        throw new ApiError(500, 'Failed to update booking');
    }
};

// Check availability for a room for a time range
export const checkAvailability = async (roomId: number, startIso: string, endIso: string): Promise<{ available: boolean; overlaps: Booking[] }> => {
    try {
        const response = await axios.get(`${BASE_URL}/api/bookings/availability`, { params: { roomId, start: startIso, end: endIso } });
        return response.data;
    } catch (error) {
        if (error instanceof ApiError) throw error;
        throw new ApiError(500, 'Failed to check availability');
    }
};

// Get suggestions for next slots and alternate rooms
export const getSuggestions = async (roomId: number, startIso: string, endIso: string, capacity?: number) => {
    try {
        const response = await axios.get(`${BASE_URL}/api/bookings/suggest`, { params: { roomId, start: startIso, end: endIso, capacity } });
        return response.data;
    } catch (error) {
        if (error instanceof ApiError) throw error;
        throw new ApiError(500, 'Failed to get suggestions');
    }
};

// Admin analytics overview
export const getAdminAnalyticsOverview = async (): Promise<any> => {
    try {
        const response = await axios.get(`${BASE_URL}/api/admin/analytics/overview`);
        return response.data;
    } catch (error) {
        if (error instanceof ApiError) throw error;
        throw new ApiError(500, 'Failed to fetch analytics');
    }
};

export default apiClient;