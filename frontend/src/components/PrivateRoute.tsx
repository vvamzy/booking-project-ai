import React from 'react';
import { Route, Redirect } from 'react-router-dom';
import { useAuthContext } from '../stores/authStore';

interface PrivateRouteProps {
  component: React.ComponentType<any>;
  path: string;
  exact?: boolean;
  requiredRole?: string; // e.g. 'ROLE_ADMIN'
}

const PrivateRoute: React.FC<PrivateRouteProps> = ({ component: Component, requiredRole, ...rest }) => {
  const { user, loading } = useAuthContext();
  if (loading) return <div>Loading...</div>;
  const hasRole = !requiredRole || (user && user.role === requiredRole);
  return (
    <Route {...rest} render={(props) => (
      user && hasRole ? <Component {...props} /> : <Redirect to={{ pathname: '/login' }} />
    )} />
  );
};

export default PrivateRoute;
