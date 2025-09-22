import React, { useState } from 'react';
import { useHistory } from 'react-router-dom';
import { useAuthContext } from '../stores/authStore';

const Login: React.FC = () => {
    const [username, setUsername] = useState('alice');
    const { login, user, loading, error } = useAuthContext();
    const history = useHistory();

    const [password, setPassword] = useState('alicepass');

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        const ok = await login({ username, password });
        if (ok) history.push('/');
    };

    if (loading) return <div>Loading...</div>;

    return (
        <div className="container py-5">
            <div className="row justify-content-center">
                <div className="col-md-6">
                    <div className="card shadow-sm">
                        <div className="card-body">
                            <h3 className="mb-3">Login</h3>
                            {error && <div className="alert alert-danger">{error}</div>}
                            <form onSubmit={handleSubmit}>
                                <div className="mb-3">
                                    <label className="form-label">Username</label>
                                    <input className="form-control" value={username} onChange={e => setUsername(e.target.value)} />
                                </div>
                                    <div className="mb-3">
                                        <label className="form-label">Password</label>
                                        <input type="password" className="form-control" value={password} onChange={e => setPassword(e.target.value)} />
                                    </div>
                                <button className="btn btn-primary" type="submit">Login</button>
                            </form>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default Login;
