import React from 'react';

import { RouteWithLayout } from './components';
import { Main as MainLayout, Minimal as MinimalLayout } from './layouts';
import { Switch, Redirect, Route } from 'react-router-dom';

import {
  LoginPage as LoginPageView,
  Dashboard as DashboardView,
  Users as UsersView,
  Services as ServicesView,
  Distribution as DistributionView,
  Logging as LoggingView,
  Failures as FailuresView,
  NotFound as NotFoundView
} from './views';
import {DistributionInfo, useDistributionInfoQuery} from './generated/graphql';

// @ts-ignore
export const LoginSwitchRoute = ({ component: Component, ...rest }) => (
  <Route {...rest} render = { props => {
    return localStorage.getItem('token')
      ? <Component {...props} />
      : <Redirect to='/login'/>
  }} />
)

interface RoutesProps {
  distributionInfo: DistributionInfo
}

const Routes: React.FC<RoutesProps> = props => {
  return (
    <Switch>
      <Redirect
        from='/'
        to='/dashboard'
        exact
      />
      <RouteWithLayout
        component={DashboardView}
        layout={MainLayout}
        path='/dashboard'
        exact
        {...props}
      />
      <RouteWithLayout
        component={UsersView}
        layout={MainLayout}
        path='/users'
        {...props}
      />
      <RouteWithLayout
        component={ServicesView}
        layout={MainLayout}
        path='/services'
        {...props}
      />
      <RouteWithLayout
        component={DistributionView}
        layout={MainLayout}
        path='/distribution'
        {...props}
      />
      <RouteWithLayout
        component={LoggingView}
        layout={MainLayout}
        path='/logging'
        exact
        {...props}
      />
      <RouteWithLayout
        component={FailuresView}
        layout={MainLayout}
        path='/failures'
        exact
        {...props}
      />
      <RouteWithLayout
        component={NotFoundView}
        layout={MinimalLayout}
        path='/not-found'
        exact
        {...props}
      />
      <Redirect to='/not-found'/>
    </Switch>
  );
}

const LoginRoutes = () => {
  const { data } = useDistributionInfoQuery()

  if (data && data.distributionInfo) {
    localStorage.setItem('distribution', data.distributionInfo.distribution)
    localStorage.setItem('distributionTitle', data.distributionInfo.title)
    return (
      <Switch>
        <RouteWithLayout
          component={LoginPageView}
          path='/login'
          layout={MinimalLayout}
          exact
        />
        <LoginSwitchRoute
          component={Routes}
          path='/'
        />
      </Switch>)
  } else {
    return null
  }
}

export default LoginRoutes;
