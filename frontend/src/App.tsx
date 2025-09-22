import React from 'react';
import { BrowserRouter as Router, Route, Switch } from 'react-router-dom';
import Dashboard from './pages/Dashboard';
import BookingPage from './pages/BookingPage';
import AdminPanel from './pages/AdminPanel';
import PrivateRoute from './components/PrivateRoute';
import Login from './pages/Login';
import Navigation from './components/Navigation';
import { BookingProvider } from './stores/bookingStore';
import AuthProvider from './stores/authStore';
import './styles/globals.css';
import { useAuthContext } from './stores/authStore';
import { Redirect, useLocation } from 'react-router-dom';

const App: React.FC = () => {
  return (
    <BookingProvider>
      <AuthProvider>
        <Router>
          <div className="app">
            <Navigation />
            <main className="main-content">
              <Switch>
                <Route path="/login" component={Login} />
                <Route path="/">
                  <AuthGate>
                    <Switch>
                      <Route exact path="/" component={Dashboard} />
                      <Route path="/book/:roomId" component={BookingPage} />
                      <Route exact path="/book" component={BookingPage} />
                      <PrivateRoute path="/admin" component={AdminPanel} requiredRole={'ROLE_ADMIN'} />
                    </Switch>
                  </AuthGate>
                </Route>
              </Switch>
            </main>
          </div>
        </Router>
      </AuthProvider>
    </BookingProvider>
  );
};

const AuthGate: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { user, loading } = useAuthContext();
  const location = useLocation();
  if (loading) return <div>Loading...</div>;
  if (!user) return <Redirect to={{ pathname: '/login', state: { from: location } }} />;
  return <>{children}</>;
};

export default App;