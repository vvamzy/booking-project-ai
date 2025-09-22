import React, { useState } from 'react';
import Navigation from '../components/Navigation';
import ApprovalQueue from '../components/ApprovalQueue';
import BookingsList from '../components/BookingsList';
import AdminAnalytics from './AdminAnalytics';

const AdminPanel: React.FC = () => {
    const [activeTab, setActiveTab] = useState<'approvals' | 'bookings' | 'analytics'>('approvals');

    return (
        <>
            <Navigation />
            <div className="container py-4">
                <h1 className="text-center mb-4">Admin Panel</h1>
                
                <ul className="nav nav-pills mb-4 justify-content-center">
                    <li className="nav-item">
                        <button 
                            className={`nav-link ${activeTab === 'approvals' ? 'active' : ''}`}
                            onClick={() => setActiveTab('approvals')}
                        >
                            Pending Approvals
                        </button>
                    </li>
                    <li className="nav-item">
                        <button 
                            className={`nav-link ${activeTab === 'bookings' ? 'active' : ''}`}
                            onClick={() => setActiveTab('bookings')}
                        >
                            All Bookings
                        </button>
                    </li>
                    <li className="nav-item">
                        <button 
                            className={`nav-link ${activeTab === 'analytics' ? 'active' : ''}`}
                            onClick={() => setActiveTab('analytics')}
                        >
                            Analytics
                        </button>
                    </li>
                </ul>

                {activeTab === 'approvals' ? (
                    <div>
                        <h2 className="text-center mb-4">Pending Approvals</h2>
                        <ApprovalQueue />
                    </div>
                ) : activeTab === 'bookings' ? (
                    <div>
                        <h2 className="text-center mb-4">All Bookings</h2>
                        <BookingsList />
                    </div>
                ) : (
                    <div>
                        <h2 className="text-center mb-4">Analytics</h2>
                        <AdminAnalytics />
                    </div>
                )}
            </div>
        </>
    );
};

export default AdminPanel;