import React, { useEffect } from 'react';
import { useParams, useHistory } from 'react-router-dom';
import BookingForm from '../components/BookingForm';
import { useBooking } from '../stores/bookingStore';
import Navigation from '../components/Navigation';

const BookingPage: React.FC = () => {
    const { roomId } = useParams<{ roomId?: string }>();
    const { state, loadRooms, loadBookings } = useBooking();
    const history = useHistory();

    useEffect(() => {
        if (!state.rooms.length || !state.bookings.length) {
            const init = async () => {
                await Promise.all([loadRooms(), loadBookings()]);
            };
            init();
        }
    }, [state.rooms.length, state.bookings.length, loadRooms, loadBookings]);

    if (state.loading) {
        return (
            <div className="container py-5">
                <div className="text-center">
                    <div className="spinner-border text-primary" role="status">
                        <span className="visually-hidden">Loading...</span>
                    </div>
                </div>
            </div>
        );
    }

    return (
        <div className="container py-4">
            <div className="d-flex justify-content-between align-items-center mb-4">
                <h1>Book a Meeting Room</h1>
                <button 
                    className="btn btn-outline-primary" 
                    onClick={() => history.push('/')}
                >
                    <i className="bi bi-arrow-left me-2"></i>
                    Back to Dashboard
                </button>
            </div>

            {state.error && (
                <div className="alert alert-danger">
                    <i className="bi bi-exclamation-triangle me-2"></i>
                    {state.error}
                </div>
            )}

            <div className="row justify-content-center">
                <div className="col-md-8">
                    <div className="card shadow-sm">
                        <div className="card-body">
                            <BookingForm 
                                onCreated={() => { history.push('/?tab=bookings'); }} 
                                initialRoomId={roomId ? Number(roomId) : undefined}
                            />
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default BookingPage;