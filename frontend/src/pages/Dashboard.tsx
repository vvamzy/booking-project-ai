import React, { useEffect, useState } from 'react';
import { useHistory } from 'react-router-dom';
import RoomCard from '../components/RoomCard';
import BookingsList from '../components/BookingsList';
import ApprovalQueue from '../components/ApprovalQueue';
import SuggestionsModal from '../components/SuggestionsModal';
import { useBooking } from '../stores/bookingStore';
import { useAuthContext } from '../stores/authStore';
import { Room } from '../types';
import * as api from '../api/apiClient';
import { toServerIsoFromLocal, parseServerDateString } from '../utils/dateUtils';

const Dashboard: React.FC = () => {
    const history = useHistory();
    const { state, loadRooms, loadBookings } = useBooking();
    const [activeTab, setActiveTab] = useState<'rooms' | 'bookings' | 'approvals'>('rooms');
    const { user } = useAuthContext();
    const isAdmin = (() => {
        if (!user) return false;
        const role = user.role || user.roles || user.authorities;
        try {
            if (typeof role === 'string') return role.toLowerCase() === 'admin' || role === 'ROLE_ADMIN';
            if (Array.isArray(role)) {
                return role.includes('admin') || role.includes('ROLE_ADMIN') || role.includes('ROLE_ADMINISTRATOR');
            }
        } catch (e) {
            return false;
        }
        // fallback: check authority-style
        if (user.authorities) {
            try {
                return user.authorities.some((a: any) => String(a?.authority || a).toUpperCase() === 'ROLE_ADMIN');
            } catch (e) { }
        }
        return false;
    })();
    const [filterStatus, setFilterStatus] = useState<'all' | 'available' | 'booked'>('all');
    const [filterDate, setFilterDate] = useState<string>('');
    const [filterStartTime, setFilterStartTime] = useState<string>('');
    const [filterLocation, setFilterLocation] = useState<string>('');
    const [availabilityMap, setAvailabilityMap] = useState<Record<number, boolean>>({});
    const [suggestions, setSuggestions] = useState<any | null>(null);
    const [showSuggestionsModal, setShowSuggestionsModal] = useState(false);
    const [suggestionRoom, setSuggestionRoom] = useState<Room | null>(null);

    useEffect(() => {
        const init = async () => {
            await Promise.all([loadRooms(), loadBookings()]);
        };
        init();
    }, [loadRooms, loadBookings]);

    // check query for tab
    useEffect(() => {
        const params = new URLSearchParams(window.location.search);
        const tab = params.get('tab');
        if (tab === 'bookings') setActiveTab('bookings');
        if (tab === 'approvals' && isAdmin) setActiveTab('approvals');
    }, [isAdmin]);

    const filterRooms = (rooms: Room[]) => {
        let res = rooms;
        if (filterDate && filterStartTime) {
            // Use availabilityMap computed from server to determine availability
            const startIso = `${filterDate}T${filterStartTime}`;
            // When availabilityMap is empty, fall back to showing all
            if (Object.keys(availabilityMap).length > 0) {
                const mapped = res.map(r => ({ ...r, _available: availabilityMap[r.id] }));
                const mappedFiltered: any[] = mapped.filter(Boolean);
                res = mappedFiltered;
            }
        }
        if (filterLocation) {
            res = res.filter(r => r.location && r.location.toLowerCase().includes(filterLocation.toLowerCase()));
        }
        switch (filterStatus) {
            case 'available':
                return res.filter((room: any) => room._available === true || (room.status && room.status.toLowerCase() === 'available'));
            case 'booked':
                return res.filter((room: any) => room._available === false || (room.status && room.status.toLowerCase() === 'booked'));
            default:
                return res;
        }
    };

    const handleRefresh = async () => {
        await Promise.all([loadRooms(), loadBookings()]);
    };

    // When user selects date/time, compute availability map
    useEffect(() => {
        const compute = async () => {
            if (!filterDate || !filterStartTime) return setAvailabilityMap({});
            const localStart = new Date(`${filterDate}T${filterStartTime}`);
            const startIso = toServerIsoFromLocal(localStart) || new Date().toISOString();
            // assume 1 hour duration by default
            const endIso = toServerIsoFromLocal(new Date(localStart.getTime() + 60 * 60 * 1000)) || new Date().toISOString();
            const map: Record<number, boolean> = {};
            await Promise.all(state.rooms.map(async (r) => {
                try {
                    const res = await api.checkAvailability(r.id, startIso, endIso);
                    map[r.id] = !!res.available;
                } catch (err) {
                    map[r.id] = true;
                }
            }));
            setAvailabilityMap(map);
        };
        compute();
    }, [filterDate, filterStartTime, state.rooms]);

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
                <h1>Smart Meeting Room System</h1>
                <button 
                    className="btn btn-outline-primary" 
                    onClick={handleRefresh}
                >
                    <i className="bi bi-arrow-clockwise me-2"></i>
                    Refresh
                </button>
            </div>

            {state.error && (
                <div className="alert alert-danger mb-4">
                    <i className="bi bi-exclamation-triangle me-2"></i>
                    {state.error}
                </div>
            )}

            <ul className="nav nav-tabs mb-4">
                <li className="nav-item">
                    <button
                        className={`nav-link ${activeTab === 'rooms' ? 'active' : ''}`}
                        onClick={() => setActiveTab('rooms')}
                    >
                        <i className="bi bi-building me-2"></i>
                        Rooms
                    </button>
                </li>
                <li className="nav-item">
                    <button
                        className={`nav-link ${activeTab === 'bookings' ? 'active' : ''}`}
                        onClick={() => setActiveTab('bookings')}
                    >
                        <i className="bi bi-calendar-event me-2"></i>
                        Bookings
                    </button>
                </li>
                {isAdmin && (
                    <li className="nav-item">
                        <button
                            className={`nav-link ${activeTab === 'approvals' ? 'active' : ''}`}
                            onClick={() => setActiveTab('approvals')}
                        >
                            <i className="bi bi-check-square me-2"></i>
                            Approvals
                        </button>
                    </li>
                )}
            </ul>

            {activeTab === 'rooms' && (
                <>
                    <div className="d-flex justify-content-between align-items-center mb-3">
                        <div>
                            <div className="d-flex g-2">
                                <input className="form-control form-control-sm me-2" type="date" value={filterDate} onChange={e => setFilterDate(e.target.value)} />
                                <input className="form-control form-control-sm me-2" type="time" value={filterStartTime} onChange={e => setFilterStartTime(e.target.value)} />
                                <input className="form-control form-control-sm" placeholder="Location" value={filterLocation} onChange={e => setFilterLocation(e.target.value)} />
                            </div>
                        </div>
                        <div className="btn-group">
                            <button
                                className={`btn btn-outline-primary ${filterStatus === 'all' ? 'active' : ''}`}
                                onClick={() => setFilterStatus('all')}
                            >
                                All Rooms
                            </button>
                            <button
                                className={`btn btn-outline-primary ${filterStatus === 'available' ? 'active' : ''}`}
                                onClick={() => setFilterStatus('available')}
                            >
                                Available
                            </button>
                            <button
                                className={`btn btn-outline-primary ${filterStatus === 'booked' ? 'active' : ''}`}
                                onClick={() => setFilterStatus('booked')}
                            >
                                Booked
                            </button>
                        </div>
                        <button 
                            className="btn btn-primary" 
                            onClick={() => history.push('/book')}
                        >
                            <i className="bi bi-plus-circle me-2"></i>
                            New Booking
                        </button>
                    </div>

                    <div className="row row-cols-1 row-cols-md-2 row-cols-lg-3 g-4">
                        {filterRooms(state.rooms).map(room => (
                            <div className="col" key={room.id}>
                                <div className="card h-100">
                                    <div className="card-body d-flex flex-column">
                                        <div className="d-flex justify-content-between align-items-start mb-2">
                                            <h5 className="card-title mb-0">{room.name}</h5>
                                            <small className="text-muted">{room.location}</small>
                                        </div>
                                        <p className="card-text text-muted">Capacity: {room.capacity}</p>
                                        <div className="mt-auto d-flex justify-content-between align-items-center">
                                            <div>
                                                {filterDate && filterStartTime && Object.keys(availabilityMap).length > 0 ? (
                                                    availabilityMap[room.id] ? (
                                                        <span className="badge bg-success">Available</span>
                                                    ) : (
                                                        <span className="badge bg-danger">Booked</span>
                                                    )
                                                ) : (
                                                    <span className="badge bg-secondary">Status: {room.status}</span>
                                                )}
                                            </div>
                                            <div>
                                                <button className="btn btn-sm btn-outline-primary me-2" onClick={() => history.push(`/book/${room.id}`)}>Book</button>
                                                <button className="btn btn-sm btn-outline-secondary" onClick={async () => {
                                                    // compute a start/end for suggestion query
                                                        const localStart = filterDate && filterStartTime ? new Date(`${filterDate}T${filterStartTime}`) : new Date();
                                                        const startIso = toServerIsoFromLocal(localStart) || new Date().toISOString();
                                                        const endIso = toServerIsoFromLocal(new Date(localStart.getTime() + 60 * 60 * 1000)) || new Date().toISOString();
                                                    try {
                                                        const s = await api.getSuggestions(room.id, startIso, endIso, room.capacity);
                                                        setSuggestions(s);
                                                        setSuggestionRoom(room);
                                                        setShowSuggestionsModal(true);
                                                    } catch (err) {
                                                        console.error('Failed to fetch suggestions', err);
                                                        alert('Could not fetch suggestions.');
                                                    }
                                                }}>View suggestions</button>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        ))}
                    </div>

                    {state.rooms.length === 0 && (
                        <div className="text-center py-5 text-muted">
                            <i className="bi bi-emoji-neutral display-4"></i>
                            <p className="mt-3">No rooms available.</p>
                        </div>
                    )}
                    <SuggestionsModal show={showSuggestionsModal} onClose={() => { setShowSuggestionsModal(false); setSuggestions(null); setSuggestionRoom(null); }} suggestions={suggestions} onQuickBook={async (roomId, startIso, endIso) => {
                        try {
                            await api.bookRoom({ roomId, userId: 0, startTime: startIso, endTime: endIso, purpose: 'Quick booking (suggestion)', attendeesCount: suggestionRoom ? Math.min(suggestionRoom.capacity, 1) : 1, requiredFacilities: [], status: 'PENDING', priority: 3, notes: '' });
                            setShowSuggestionsModal(false);
                            setSuggestions(null);
                            setSuggestionRoom(null);
                            await loadRooms();
                            await loadBookings();
                        } catch (err) {
                            console.error('Quick book failed', err);
                            alert('Quick book failed');
                        }
                    }} />
                </>
            )}

            {activeTab === 'bookings' && (
                <BookingsList showAiDecisions={true} />
            )}

            {activeTab === 'approvals' && (
                isAdmin ? (
                    <ApprovalQueue onApprovalUpdate={handleRefresh} />
                ) : (
                    <div className="alert alert-warning">
                        You are not authorized to view approvals.
                    </div>
                )
            )}
        </div>
    );
};

export default Dashboard;