import React from 'react';
import { Switch, Redirect } from 'react-router-dom';

import { RouteWithLayout } from './components';
import { Main as MainLayout, Minimal as MinimalLayout } from './layouts';

import { Route } from 'react-router-dom';

import {
  LoginPage as LoginPageView,
  Dashboard as DashboardView,
  Services as ServicesView,
  Clients as ClientsView,
  Logging as LoggingView,
  Failures as FailuresView,
  NotFound as NotFoundView
} from './views';

export const LoginSwitchRoute = ({ component: Component, ...rest }) => (
  <Route {...rest} render = { props => (
    localStorage.getItem('user')
      ? <Component {...props} />
      : <Redirect to="/login" />
  )} />
)

const LoginRoutes = () => {
  return (
      <Switch>
        <RouteWithLayout
          component={LoginPageView}
          exact
          layout={MinimalLayout}
          path="/login"
        />
        <LoginSwitchRoute path="/" component={Routes} />
      </Switch>
  );
}

const Routes = () => {
  return (
    <Switch>
      <Redirect
        exact
        from="/"
        to="/dashboard"
      />
      <RouteWithLayout
        component={DashboardView}
        exact
        layout={MainLayout}
        path="/dashboard"
      />
      <RouteWithLayout
        component={ServicesView}
        exact
        layout={MainLayout}
        path="/services"
      />
      <RouteWithLayout
        component={ClientsView}
        exact
        layout={MainLayout}
        path="/clients"
      />
      <RouteWithLayout
        component={LoggingView}
        exact
        layout={MainLayout}
        path="/logging"
      />
      <RouteWithLayout
        component={FailuresView}
        exact
        layout={MainLayout}
        path="/failures"
      />
      <RouteWithLayout
        component={NotFoundView}
        exact
        layout={MinimalLayout}
        path="/not-found"
      />
      <Redirect to="/not-found" />
    </Switch>
  );
};

export default LoginRoutes;
