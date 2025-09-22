import React, { useEffect, useState } from 'react';
import { bookRoom } from '../api/apiClient';
import { Room, Equipment, BookingRequest } from '../types';
import SuggestionsModal from './SuggestionsModal';
import PurposeClarifyModal from './PurposeClarifyModal';
import { toServerIsoFromLocal } from '../utils/dateUtils';
import { useBooking } from '../stores/bookingStore';
import { useHistory } from 'react-router-dom';
import '../styles/BookingForm.css';

type BookingFormProps = {
    onCreated?: () => void;
    initialRoomId?: number;
};

const BookingForm: React.FC<BookingFormProps> = ({ onCreated, initialRoomId }) => {
    const [selectedRoomId, setSelectedRoomId] = useState<number | undefined>(initialRoomId);
    const [date, setDate] = useState(new Date().toISOString().split('T')[0]);
    const [startTime, setStartTime] = useState('09:00');
    const [endTime, setEndTime] = useState('10:00');
    const [purpose, setPurpose] = useState('');
    const [attendees, setAttendees] = useState<number>(1);
    const [selectedEquipment, setSelectedEquipment] = useState<string[]>([]);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [message, setMessage] = useState<{ type: 'success' | 'error' | 'info'; text: string } | null>(null);
    const [suggestions, setSuggestions] = useState<any | null>(null);
    const [showSuggestionsModal, setShowSuggestionsModal] = useState(false);
    const [showPurposeModal, setShowPurposeModal] = useState(false);
    const [purposeRationale, setPurposeRationale] = useState<string[] | undefined>(undefined);
    const { state: { rooms }, loadRooms } = useBooking();
    const history = useHistory();

    useEffect(() => {
        if (rooms.length === 0) {
            loadRooms();
        } else if (!selectedRoomId && rooms.length > 0) {
            setSelectedRoomId(rooms[0].id);
        }
    }, [rooms, loadRooms, selectedRoomId]);

    const validateForm = (): string | null => {
        if (!selectedRoomId) {
            return 'Please select a room';
        }

        const selectedRoom = rooms.find(r => r.id === selectedRoomId);
        if (!selectedRoom) {
            return 'Invalid room selected';
        }

    const start = new Date(`${date}T${startTime}`);
    const end = new Date(`${date}T${endTime}`);

        if (end <= start) {
            return 'End time must be after start time';
        }

        if (start < new Date()) {
            return 'Cannot book a room in the past';
        }

        if (attendees > selectedRoom.capacity) {
            return `Room capacity (${selectedRoom.capacity}) exceeded`;
        }

        if (purpose.length < 10) {
            return 'Please provide a more detailed purpose (minimum 10 characters)';
        }

        return null;
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setMessage(null);
        setIsSubmitting(true);

        const validationError = validateForm();
        if (validationError) {
            setMessage({ type: 'error', text: validationError });
            setIsSubmitting(false);
            return;
        }

        if (!selectedRoomId) {
            setMessage({ type: 'error', text: 'Please select a room' });
            setIsSubmitting(false);
            return;
        }

        try {
            const start = new Date(`${date}T${startTime}`);
            const end = new Date(`${date}T${endTime}`);

            const bookingRequest: BookingRequest = {
                roomId: selectedRoomId,
                startTime: toServerIsoFromLocal(start)?? '',
                endTime: toServerIsoFromLocal(end)??'',
                purpose,
                attendeesCount: attendees,
                notes: '',
                requiredFacilities: selectedEquipment,
                status: 'PENDING',
                priority: 3 // Default priority
            };

            // Validate purpose/booking using AI/rules before persisting
            const api = await import('../api/apiClient');
            try {
                const val = await api.validateBooking(bookingRequest);
                if (val && val.valid === false) {
                    // store rationale and show purpose clarification modal so the user can edit without losing form state
                    setPurposeRationale(val.rationale || []);
                    setShowPurposeModal(true);
                    setIsSubmitting(false);
                    return;
                }
            } catch (e) {
                // validation failed due to API error: continue to availability check but log
                console.warn('Validation call failed, continuing to availability check', e);
            }

            // Check availability first
            const avail = await api.checkAvailability(selectedRoomId, bookingRequest.startTime, bookingRequest.endTime);
            if (!avail.available) {
                // get suggestions
                const s = await api.getSuggestions(selectedRoomId, bookingRequest.startTime, bookingRequest.endTime, attendees);
                setSuggestions(s);
                setShowSuggestionsModal(true);
                setMessage({ type: 'error', text: 'Selected room is not available at requested time. Suggestions provided below.' });
                setIsSubmitting(false);
                return;
            }

            const created = await bookRoom(bookingRequest);
            setMessage({ 
                type: 'success', 
                text: `Booking created successfully! Status: ${created.status}${created.aiDecision ? ' | AI Status: ' + created.aiDecision.decision : ''}` 
            });
            
            // Reset form
            setPurpose('');
            setAttendees(1);
            setSelectedEquipment([]);
            setDate(new Date().toISOString().split('T')[0]);
            setStartTime('09:00');
            setEndTime('10:00');
            
            if (onCreated) onCreated();
            // Redirect user to their bookings list so they can see status immediately
            history.push('/?tab=bookings');
        } catch (err) {
            console.error('booking failed', err);
            setMessage({ 
                type: 'error', 
                text: err instanceof Error ? err.message : 'Failed to create booking' 
            });
        } finally {
            setIsSubmitting(false);
        }
    };

    // Called by PurposeClarifyModal: update purpose and re-run submit flow (validation then availability/create)
    const handlePurposeConfirm = async (newPurpose: string) => {
        setShowPurposeModal(false);
        setPurpose(newPurpose);
        // Re-run submit sequence programmatically
        // We call the same logic as handleSubmit but without event
        setMessage(null);
        setIsSubmitting(true);
        try {
            const start = new Date(`${date}T${startTime}`);
            const end = new Date(`${date}T${endTime}`);
            const bookingRequest: BookingRequest = {
                roomId: selectedRoomId??0,
                userId: 0,
                startTime: toServerIsoFromLocal(start) || '',
                endTime: toServerIsoFromLocal(end) || '',
                purpose: newPurpose,
                attendeesCount: attendees,
                notes: '',
                requiredFacilities: selectedEquipment,
                status: 'PENDING',
                priority: 3
            };
            const api = await import('../api/apiClient');
            const val = await api.validateBooking(bookingRequest);
            if (val && val.valid === false) {
                // still invalid — show message and keep modal closed so user can edit again
                setMessage({ type: 'error', text: 'Purpose still flagged as invalid: ' + (val.rationale ? val.rationale.join('; ') : '') });
                setPurposeRationale(val.rationale || []);
                setShowPurposeModal(true);
                setIsSubmitting(false);
                return;
            }
            // availability
            const avail = await api.checkAvailability(selectedRoomId??0, bookingRequest.startTime, bookingRequest.endTime);
            if (!avail.available) {
                const s = await api.getSuggestions(selectedRoomId??0, bookingRequest.startTime, bookingRequest.endTime, attendees);
                setSuggestions(s);
                setShowSuggestionsModal(true);
                setMessage({ type: 'error', text: 'Selected room is not available at requested time. Suggestions provided below.' });
                setIsSubmitting(false);
                return;
            }
            const created = await bookRoom(bookingRequest);
            setMessage({ type: 'success', text: `Booking created successfully! Status: ${created.status}` });
            if (onCreated) onCreated();
            history.push('/?tab=bookings');
        } catch (err) {
            console.error('booking failed', err);
            setMessage({ type: 'error', text: err instanceof Error ? err.message : 'Failed to create booking' });
        } finally {
            setIsSubmitting(false);
        }
    };

    const selectedRoom = rooms.find(r => r.id === selectedRoomId);

    const handleQuickBook = async (roomId: number, startIso: string, endIso: string) => {
        setIsSubmitting(true);
        try {
            const quickReq: BookingRequest = {
                roomId,
                startTime: startIso,
                endTime: endIso,
                purpose,
                attendeesCount: attendees,
                requiredFacilities: selectedEquipment,
                status: 'PENDING',
                priority: 3,
                notes: ''
            };
            await bookRoom(quickReq);
            setShowSuggestionsModal(false);
            setSuggestions(null);
            if (onCreated) onCreated();
            history.push('/?tab=bookings');
        } catch (err) {
            console.error('Quick book failed', err);
        } finally {
            setIsSubmitting(false);
        }
    };

    return (
        <form onSubmit={handleSubmit} className="booking-form">
            <div className="mb-4">
                <label className="form-label fw-semibold">Room:</label>
                <select 
                    className="form-select form-select-lg shadow-sm" 
                    value={selectedRoomId || ''} 
                    onChange={(e) => setSelectedRoomId(Number(e.target.value))} 
                    required
                    disabled={isSubmitting}
                >
                    <option value="">Select a room</option>
                    {rooms.map(r => (
                        <option key={r.id} value={r.id}>
                            {r.name} ({r.location}) - Capacity: {r.capacity}
                        </option>
                    ))}
                </select>
                {selectedRoom && (
                    <div className="text-muted mt-2">
                        <small>
                            Available equipment: {selectedRoom.equipment.map(e => e.name).join(', ')}
                        </small>
                    </div>
                )}
            </div>

            <div className="mb-4">
                <label className="form-label fw-semibold">Meeting Purpose:</label>
                <textarea
                    className="form-control form-control-lg shadow-sm"
                    value={purpose}
                    onChange={(e) => setPurpose(e.target.value)}
                    required
                    disabled={isSubmitting}
                    placeholder="Describe the purpose of your meeting..."
                    rows={3}
                    minLength={10}
                />
                <small className="text-muted">
                    Minimum 10 characters required
                </small>
            </div>

            <div className="mb-4">
                <label className="form-label fw-semibold">Number of Attendees:</label>
                <input
                    type="number"
                    className="form-control form-control-lg shadow-sm"
                    value={attendees}
                    onChange={(e) => setAttendees(Math.max(1, Number(e.target.value)))}
                    min="1"
                    max={selectedRoom?.capacity || 999}
                    required
                    disabled={isSubmitting}
                />
                {selectedRoom && (
                    <small className="text-muted">
                        Room capacity: {selectedRoom.capacity} people
                    </small>
                )}
            </div>

            <div className="row mb-4">
                <div className="col-md-4">
                    <label className="form-label fw-semibold">Date:</label>
                    <input 
                        type="date" 
                        className="form-control form-control-lg shadow-sm" 
                        value={date} 
                        onChange={(e) => setDate(e.target.value)} 
                        required 
                        disabled={isSubmitting}
                        min={new Date().toISOString().split('T')[0]}
                    />
                </div>
                <div className="col-md-4">
                    <label className="form-label fw-semibold">Start Time:</label>
                    <input 
                        type="time" 
                        className="form-control form-control-lg shadow-sm" 
                        value={startTime} 
                        onChange={(e) => setStartTime(e.target.value)} 
                        required 
                        disabled={isSubmitting}
                    />
                </div>
                <div className="col-md-4">
                    <label className="form-label fw-semibold">End Time:</label>
                    <input 
                        type="time"
                        className="form-control form-control-lg shadow-sm" 
                        value={endTime} 
                        onChange={(e) => setEndTime(e.target.value)} 
                        required 
                        disabled={isSubmitting}
                    />
                </div>
            </div>

            {selectedRoom?.equipment && selectedRoom.equipment.length > 0 && (
                <div className="mb-4">
                    <label className="form-label fw-semibold">Required Equipment:</label>
                    <div className="row g-3">
                        {selectedRoom.equipment.map(item => (
                            <div key={item.id} className="col-md-6 col-lg-4">
                                <div className="form-check">
                                    <input
                                        type="checkbox"
                                        className="form-check-input"
                                        id={`equipment-${item.id}`}
                                        checked={selectedEquipment.includes(item.id)}
                                        onChange={(e) => {
                                            if (e.target.checked) {
                                                setSelectedEquipment([...selectedEquipment, item.id]);
                                            } else {
                                                setSelectedEquipment(selectedEquipment.filter(id => id !== item.id));
                                            }
                                        }}
                                        disabled={isSubmitting || (item.status && item.status.toLowerCase() !== 'available')}
                                    />
                                    <label className="form-check-label" htmlFor={`equipment-${item.id}`}>
                                        {item.name}
                                        <span className={`ms-2 badge ${
                                            item.status === 'available' ? 'bg-success' :
                                            item.status === 'in-use' ? 'bg-warning' :
                                            'bg-danger'
                                        }`}>
                                            {item.status}
                                        </span>
                                    </label>
                                </div>
                            </div>
                        ))}
                    </div>
                </div>
            )}
            <SuggestionsModal show={showSuggestionsModal} onClose={() => { setShowSuggestionsModal(false); setSuggestions(null); }} suggestions={suggestions} onQuickBook={handleQuickBook} />
            <PurposeClarifyModal show={showPurposeModal} initialPurpose={purpose} rationale={purposeRationale} onClose={() => setShowPurposeModal(false)} onConfirm={handlePurposeConfirm} />

            <div className="d-grid gap-2">
                <button 
                    type="submit" 
                    className="btn btn-primary btn-lg shadow transition-all hover-scale"
                    disabled={isSubmitting}
                >
                    {isSubmitting ? (
                        <>
                            <span className="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
                            Submitting...
                        </>
                    ) : (
                        'Book Room'
                    )}
                </button>
            </div>

            {message && (
                <div className={`alert alert-${message.type === 'error' ? 'danger' : 'success'} mt-4`}>
                    {message.text}
                </div>
            )}
        </form>
    );
};

export default BookingForm;