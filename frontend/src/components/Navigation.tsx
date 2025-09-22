import React from 'react';
import { useLocation, Link } from 'react-router-dom';
import { useAuthContext } from '../stores/authStore';

const Navigation: React.FC = () => {
    const location = useLocation();
    
    const { user, logout } = useAuthContext();

    return (
        <div className="container-fluid bg-light mb-4">
            <div className="container">
                <ul className="nav nav-tabs justify-content-center">
                    <li className="nav-item">
                        <Link 
                            to="/" 
                            className={`nav-link ${location.pathname === '/' ? 'active' : ''}`}
                        >
                            Dashboard
                        </Link>
                    </li>
                    <li className="nav-item">
                        <Link 
                            to="/book" 
                            className={`nav-link ${location.pathname.startsWith('/book') ? 'active' : ''}`}
                        >
                            Book Room
                        </Link>
                    </li>
                    {user ? (
                        <li className="nav-item ms-auto d-flex align-items-center">
                            <span className="me-3">{user.username}</span>
                            <button className="btn btn-sm btn-outline-secondary" onClick={() => logout()}>Logout</button>
                        </li>
                    ) : (
                        <li className="nav-item ms-auto">
                            <Link to="/login" className={`nav-link ${location.pathname === '/login' ? 'active' : ''}`}>Login</Link>
                        </li>
                    )}
                    {user && (user.role === 'ROLE_ADMIN' || user.role === 'ADMIN') && (
                        <li className="nav-item">
                            <Link 
                                to="/admin" 
                                className={`nav-link ${location.pathname === '/admin' ? 'active' : ''}`}
                            >
                                Admin Panel
                            </Link>
                        </li>
                    )}
                </ul>
            </div>
        </div>
    );
};

export default Navigation;