import React from 'react';

import { RouteWithLayout } from './components';
import { Main as MainLayout, Minimal as MinimalLayout } from './layouts';
import { Switch, Redirect, Route } from 'react-router-dom';

import {
  LoginPage as LoginPageView,
  Dashboard as DashboardView,
  Services as ServicesView,
  Clients as ClientsView,
  Logging as LoggingView,
  Failures as FailuresView,
  NotFound as NotFoundView
} from './views';
import {DistributionInfo, useDistributionInfoQuery} from "./generated/graphql";

// @ts-ignore
export const LoginSwitchRoute = ({ component: Component, distributionInfo, ...rest }) => (
  <Route {...rest} render = { props => {
    return localStorage.getItem('token')
      ? <Component distributionName={distributionInfo} {...props}/>
      : <Redirect to='/login'/>
  }} />
)

interface RoutesProps {
  distributionInfo: DistributionInfo
}

const Routes: React.FC<RoutesProps> = props => {
  const { distributionInfo, ...rest } = props;
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
        distributionInfo={distributionInfo}
        {...rest}
      />
      <RouteWithLayout
        component={ServicesView}
        layout={MainLayout}
        path='/services'
        distributionInfo={distributionInfo}
        exact
        {...rest}
      />
      <RouteWithLayout
        component={ClientsView}
        layout={MainLayout}
        path='/clients'
        exact
        distributionInfo={distributionInfo}
        {...rest}
      />
      <RouteWithLayout
        component={LoggingView}
        layout={MainLayout}
        path='/logging'
        exact
        distributionInfo={distributionInfo}
        {...rest}
      />
      <RouteWithLayout
        component={FailuresView}
        layout={MainLayout}
        path='/failures'
        exact
        distributionInfo={distributionInfo}
        {...rest}
      />
      <RouteWithLayout
        component={NotFoundView}
        layout={MinimalLayout}
        path='/not-found'
        exact
        distributionInfo={distributionInfo}
        {...rest}
      />
      <Redirect to='/not-found'/>
    </Switch>
  );
}

const LoginRoutes = () => {
  const { data } = useDistributionInfoQuery()

  // @ts-ignore
  if (data && data.distributionInfo) {
    return (
      <Switch>
        <RouteWithLayout
          component={LoginPageView}
          path='/login'
          layout={MinimalLayout}
          distributionInfo={data.distributionInfo}
          exact
        />
        <LoginSwitchRoute
          path='/'
          component={Routes}
          distributionInfo={data.distributionInfo}
        />
      </Switch>)
  } else {
    return null
  }
}

export default LoginRoutes;
