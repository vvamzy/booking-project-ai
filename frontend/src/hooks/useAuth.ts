import { useState, useEffect } from 'react';

const useAuth = () => {
    const [user, setUser] = useState<any>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        const fetchUser = async () => {
            try {
                const API_ROOT = process.env.REACT_APP_API_URL || 'http://localhost:8080';
                const response = await fetch(`${API_ROOT}/api/auth/user`, { credentials: 'include' });
                if (response.status === 401 || response.status === 204) {
                    setUser(null);
                    return;
                }
                if (!response.ok) {
                    throw new Error('Failed to fetch user');
                }
                const data = await response.json();
                setUser(data);
            } catch (err) {
                const msg = err instanceof Error ? err.message : String(err);
                setError(msg);
            } finally {
                setLoading(false);
            }
        };

        fetchUser();
    }, []);

    const login = async (credentials: any) => {
        setLoading(true);
        try {
            const API_ROOT = process.env.REACT_APP_API_URL || 'http://localhost:8080';
            const response = await fetch(`${API_ROOT}/api/auth/login`, {
                method: 'POST',
                credentials: 'include',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(credentials),
            });
            if (!response.ok) {
                const txt = await response.text().catch(() => 'Login failed');
                throw new Error(txt || 'Login failed');
            }
            const data = await response.json();
            setUser(data);
            return true;
        } catch (err) {
            const msg = err instanceof Error ? err.message : String(err);
            setError(msg);
            return false;
        } finally {
            setLoading(false);
        }
    };

    const logout = async () => {
        setLoading(true);
        try {
            const API_ROOT = process.env.REACT_APP_API_URL || 'http://localhost:8080';
            await fetch(`${API_ROOT}/api/auth/logout`, { method: 'POST', credentials: 'include' });
            setUser(null);
        } catch (err) {
            const msg = err instanceof Error ? err.message : String(err);
            setError(msg);
        } finally {
            setLoading(false);
        }
    };

    return { user, loading, error, login, logout };
};

export default useAuth;