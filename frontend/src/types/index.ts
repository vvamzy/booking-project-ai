export interface Room {
    id: number;
    name: string;
    capacity: number;
    location: string;
    equipment: Equipment[];
    status: 'available' | 'booked' | 'maintenance';
}

export interface Equipment {
    id: string;
    name: string;
    type: string;
    status: 'available' | 'in-use' | 'maintenance';
}

export interface Booking {
    id: number;
    roomId: number;
    userId: number;
    startTime: string;
    endTime: string;
    purpose: string;
    attendeesCount: number;
    requiredFacilities: string[];
    status: 'PENDING' | 'APPROVED' | 'REJECTED';
    priority: number;
    notes?: string;
    aiDecision?: {
        decision: 'APPROVE' | 'REJECT';
        confidence: number;
        rationale: string;
    };
}

export interface BookingRequest {
    roomId: number;
    userId?: number;
    startTime: string;
    endTime: string;
    purpose: string;
    attendeesCount: number;
    requiredFacilities: string[];
    status: string;
    priority: number;
    notes?: string;
}

export interface User {
    id: number;
    username: string;
    email: string;
    role: 'user' | 'admin';
}

export interface Approval {
    id: number;
    bookingId: number;
    approverId: number;
    decision: 'approved' | 'rejected';
    comments?: string;
    aiDecision?: Booking['aiDecision'];
    timestamp: string;
}