import React from 'react';

import { Room } from '../types';

interface RoomCardProps {
    room: Room;
    onBook: () => void;
}

const RoomCard: React.FC<RoomCardProps> = ({ room, onBook }) => {
    const statusClass = room.status === 'available' ? 'text-success' : 'text-danger';
    const statusIcon = room.status === 'available' ? 'bi-check-circle' : 'bi-x-circle';

    return (
        <div className="card h-100">
            <div className="card-body">
                <div className="d-flex justify-content-between align-items-start mb-3">
                    <h5 className="card-title mb-0">{room.name}</h5>
                    <span className={`badge ${statusClass}`}>
                        <i className={`bi ${statusIcon} me-1`}></i>
                        {room.status 
                            ? room.status.charAt(0).toUpperCase() + room.status.slice(1) 
                            : 'Unknown'}
                    </span>
                </div>
                <div className="card-text">
                    <p className="mb-2">
                        <i className="bi bi-people me-2"></i>
                        <strong>Capacity:</strong> {room.capacity} people
                    </p>
                    <p className="mb-2">
                        <i className="bi bi-geo-alt me-2"></i>
                        <strong>Location:</strong> {room.location}
                    </p>
                    {room.equipment && room.equipment.length > 0 && (
                        <p className="mb-2">
                            <i className="bi bi-tools me-2"></i>
                            <strong>Equipment:</strong>
                            <ul className="list-unstyled ms-4 mb-0">
                                {room.equipment.map((item, index) => (
                                    <li key={item.id}>
                                        <i className="bi bi-dot me-1"></i>
                                        {item.name}
                                        <span className={`ms-2 badge ${
                                            item.status === 'available' ? 'bg-success' :
                                            item.status === 'in-use' ? 'bg-warning' : 
                                            'bg-danger'
                                        }`}>
                                            {item.status}
                                        </span>
                                    </li>
                                ))}
                            </ul>
                        </p>
                    )}
                </div>
                <button 
                    className="btn btn-primary w-100 mt-3" 
                    onClick={onBook}
                    disabled={room.status !== 'available'}
                >
                    <i className="bi bi-calendar-plus me-2"></i>
                    {room.status === 'available' ? 'Book Room' : 'Currently Booked'}
                </button>
            </div>
        </div>
    );
};

export default RoomCard;