import React, { useEffect, useState } from 'react';
import { getAllBookings, getRooms, getAiDecision } from '../api/apiClient';
import { Booking, Room } from '../types';
import { formatDateLocal, formatTimeLocal } from '../utils/dateUtils';
import '../styles/BookingsList.css';

type Props = {
    refreshKey?: number;
    showAiDecisions?: boolean;
}

const BookingsList: React.FC<Props> = ({ refreshKey, showAiDecisions = true }) => {
    const [bookings, setBookings] = useState<Booking[]>([]);
    const [rooms, setRooms] = useState<Record<number, Room>>({});
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    const load = async () => {
        setLoading(true);
        setError(null);
        try {
            const [bookingsData, roomsData] = await Promise.all([
                getAllBookings(),
                getRooms()
            ]);

            const roomsMap: Record<number, Room> = {};
            roomsData.forEach((room: Room) => roomsMap[room.id] = room);
            
            // If AI decisions are enabled, fetch them for pending bookings
            if (showAiDecisions) {
                const pendingStatuses = ['PENDING', 'PENDING_APPROVAL'];
                const pendingBookings = bookingsData.filter(b => pendingStatuses.includes(String(b.status)));
                await Promise.all(
                    pendingBookings.map(async (booking) => {
                        try {
                            const aiDecision = await getAiDecision(booking.id);
                            if (aiDecision) {
                                booking.aiDecision = {
                                    decision: aiDecision.decision,
                                    confidence: aiDecision.confidence,
                                    rationale: aiDecision.rationale
                                };
                            }
                        } catch (err) {
                            console.warn(`Could not fetch AI decision for booking ${booking.id}:`, err);
                        }
                    })
                );
            }

            setBookings(bookingsData);
            setRooms(roomsMap);
        } catch (err) {
            console.error('Failed to load bookings:', err);
            setError('Failed to load bookings. Please try again.');
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => { 
        load(); 
    }, [refreshKey, showAiDecisions]);

    if (loading) {
        return (
            <div className="text-center p-4">
                <div className="spinner-border text-primary" role="status">
                    <span className="visually-hidden">Loading...</span>
                </div>
            </div>
        );
    }

    if (error) {
        return (
            <div className="alert alert-danger">
                {error}
            </div>
        );
    }

    if (!bookings || bookings.length === 0) {
        return (
            <div className="alert alert-info text-center">
                No bookings found.
            </div>
        );
    }

    return (
        <div className="bookings-list">
            <div className="table-responsive">
                <table className="table table-bordered bookings-table">
                    <thead>
                        <tr>
                            <th>Booking ID</th>
                            <th>Room</th>
                            <th>Time</th>
                            <th>Purpose</th>
                            <th>Attendees</th>
                            <th>Status</th>
                            {showAiDecisions && <th>AI Decision</th>}
                            <th>Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        {bookings.map(booking => {
                            const room = rooms[booking.roomId];
                            return (
                                <tr key={booking.id} className={`booking-row ${booking.status}`}>
                                    <td className="fw-semibold">#{booking.id}</td>
                                    <td>
                                        <div>{room?.name || booking.roomId}</div>
                                        {room && <small className="text-muted d-block">{room.location}</small>}
                                    </td>
                                    <td>
                                        <div>{formatDateLocal(booking.startTime)}</div>
                                        <small className="text-muted d-block">
                                            {formatTimeLocal(booking.startTime)} - {formatTimeLocal(booking.endTime)}
                                        </small>
                                    </td>
                                    <td>
                                        <div className="purpose-cell">
                                            {booking.purpose}
                                            {booking.requiredFacilities.length > 0 && (
                                                <small className="text-muted d-block">
                                                    Equipment: {booking.requiredFacilities.map(eqId => 
                                                        room?.equipment.find(e => e.id === eqId)?.name
                                                    ).filter(Boolean).join(', ')}
                                                </small>
                                            )}
                                        </div>
                                    </td>
                                    <td className="text-center">
                                        {booking.attendeesCount}
                                        {room && (
                                            <small className="text-muted d-block">
                                                of {room.capacity}
                                            </small>
                                        )}
                                    </td>
                                    <td>
                                        <span className={`status-badge badge ${
                                            booking.status === 'APPROVED' ? 'bg-success' :
                                            booking.status === 'REJECTED' ? 'bg-danger' :
                                            'bg-warning'
                                        }`}>
                                            {booking.status.toUpperCase()}
                                        </span>
                                    </td>
                                    {showAiDecisions && (
                                        <td>
                                            {booking.aiDecision ? (
                                                <div className="ai-decision">
                                                    <span className={`badge ${
                                                        booking.aiDecision.decision === 'APPROVE' ? 'bg-success' : 'bg-danger'
                                                    }`}>
                                                        {booking.aiDecision.decision.toUpperCase()}
                                                    </span>
                                                    <small className="d-block mt-1">
                                                        Confidence: {(booking.aiDecision.confidence * 100).toFixed(1)}%
                                                    </small>
                                                    <button
                                                        className="btn btn-link btn-sm p-0 mt-1"
                                                        onClick={() => {
                                                            // TODO: Show AI rationale in a modal
                                                            alert(booking.aiDecision?.rationale);
                                                        }}
                                                    >
                                                        View Rationale
                                                    </button>
                                                </div>
                                            ) : booking.status === 'PENDING' ? (
                                                <span className="text-muted">Analyzing...</span>
                                            ) : (
                                                <span className="text-muted">N/A</span>
                                            )}
                                        </td>
                                    )}
                                    <td className="text-end">
                                        <button className="btn btn-sm btn-outline-secondary me-2" onClick={async () => {
                                            const newPurpose = prompt('Edit purpose', booking.purpose || '');
                                            if (newPurpose !== null) {
                                                try {
                                                    const api = await import('../api/apiClient');
                                                    await api.updateBooking(booking.id, { purpose: newPurpose });
                                                    await load();
                                                } catch (err) {
                                                    alert('Failed to update booking');
                                                }
                                            }
                                        }}>Edit</button>
                                        <button className="btn btn-sm btn-outline-danger" onClick={async () => {
                                            if (!confirm('Cancel this booking?')) return;
                                            try {
                                                const api = await import('../api/apiClient');
                                                await api.cancelBooking(booking.id, 'user');
                                                await load();
                                            } catch (err) {
                                                alert('Failed to cancel booking');
                                            }
                                        }}>Cancel</button>
                                    </td>
                                </tr>
                            );
                        })}
                    </tbody>
                </table>
            </div>
        </div>
    );
};

export default BookingsList;
