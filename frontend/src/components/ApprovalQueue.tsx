import React, { useState } from 'react';
import { Booking } from '../types';
import { formatDateTimeLocal } from '../utils/dateUtils';
import { useBooking } from '../stores/bookingStore';

interface ApprovalQueueProps {
    onApprovalUpdate?: () => void;
}

const ApprovalQueue: React.FC<ApprovalQueueProps> = ({ onApprovalUpdate }) => {
    const { state: { bookings, rooms, loading, error }, handleBookingApproval } = useBooking();
    const [processing, setProcessing] = useState<Record<number, boolean>>({});

    const pendingStatuses = ['PENDING', 'PENDING_APPROVAL'];
    const pendingBookings = bookings.filter(b => pendingStatuses.includes(String(b.status)));

    const handleBookingAction = async (bookingId: number, action: 'APPROVE' | 'REJECT', comments?: string) => {
        setProcessing(prev => ({ ...prev, [bookingId]: true }));
        try {
            await handleBookingApproval(bookingId, action, comments);
            if (onApprovalUpdate) {
                onApprovalUpdate();
            }
        } catch (err) {
            console.error(`Failed to ${action} booking:`, err);
        } finally {
            setProcessing(prev => ({ ...prev, [bookingId]: false }));
        }
    };

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
                <i className="bi bi-exclamation-triangle me-2"></i>
                {error}
            </div>
        );
    }

    if (pendingBookings.length === 0) {
        return (
            <div className="alert alert-info text-center">
                <i className="bi bi-check-circle me-2"></i>
                No pending approvals at the moment.
            </div>
        );
    }

    return (
        <div className="approval-queue">
            {pendingBookings.map((booking) => {
                const room = rooms.find(r => r.id === booking.roomId);
                const isProcessing = processing[booking.id] || false;
                const equipmentList = room?.equipment
                    .filter(e => booking.requiredFacilities.includes(e.id))
                    .map(e => e.name)
                    .join(', ');

                return (
                    <div key={booking.id} className="card mb-3 shadow-sm">
                        <div className="card-header d-flex justify-content-between align-items-center">
                            <h5 className="mb-0">Booking Request #{booking.id}</h5>
                            <span className="badge bg-warning">Pending</span>
                        </div>
                        <div className="card-body">
                            <div className="row">
                                <div className="col-md-8">
                                    <div className="mb-3">
                                        <h6>Room Details</h6>
                                        <p className="mb-1">
                                            <strong>{room?.name || 'Unknown Room'}</strong> 
                                            {room?.location && ` (${room.location})`}
                                        </p>
                                        <small className="text-muted">
                                            Attendees: {booking.attendeesCount} {room && `of ${room.capacity} capacity`}
                                        </small>
                                    </div>

                                    <div className="mb-3">
                                        <h6>Booking Details</h6>
                                        <p className="mb-1">
                                            <strong>Time:</strong> {formatDateTimeLocal(booking.startTime)} - {formatDateTimeLocal(booking.endTime)}
                                        </p>
                                        <p className="mb-1">
                                            <strong>Purpose:</strong> {booking.purpose}
                                        </p>
                                        {equipmentList && (
                                            <p className="mb-1">
                                                <strong>Equipment:</strong> {equipmentList}
                                            </p>
                                        )}
                                    </div>

                                    {booking.aiDecision && (
                                        <div className={`alert ${
                                            booking.aiDecision.decision === 'APPROVE' 
                                                ? 'alert-success' 
                                                : 'alert-danger'
                                        } mb-3`}>
                                            <h6>AI Recommendation</h6>
                                            <p className="mb-1">
                                                <strong>Decision:</strong>{' '}
                                                {booking.aiDecision.decision.toUpperCase()}
                                                <span className="ms-2">
                                                    (Confidence: {booking.aiDecision.confidence.toFixed(1)}%)
                                                </span>
                                            </p>
                                            <p className="mb-0">
                                                <strong>Rationale:</strong> {booking.aiDecision.rationale}
                                            </p>
                                        </div>
                                    )}
                                </div>

                                <div className="col-md-4">
                                    <div className="d-grid gap-2">
                                        <button
                                            className="btn btn-success"
                                            onClick={() => handleBookingAction(booking.id, 'APPROVE')}
                                            disabled={isProcessing}
                                        >
                                            {isProcessing ? (
                                                <>
                                                    <span className="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
                                                    Processing...
                                                </>
                                            ) : (
                                                <>
                                                    <i className="bi bi-check-circle me-2"></i>
                                                    Approve
                                                </>
                                            )}
                                        </button>
                                        <button
                                            className="btn btn-danger"
                                            onClick={() => handleBookingAction(booking.id, 'REJECT')}
                                            disabled={isProcessing}
                                        >
                                            {isProcessing ? (
                                                <>
                                                    <span className="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
                                    Processing...
                                </>
                            ) : (
                                <>
                                    <i className="bi bi-x-circle me-2"></i>
                                    Reject
                                </>
                            )}
                        </button>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                );
            })}
        </div>
    );
};

export default ApprovalQueue;