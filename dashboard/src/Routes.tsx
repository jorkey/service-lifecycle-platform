import React from 'react';

import { RouteWithLayout } from './components';
import { Main as MainLayout, Minimal as MinimalLayout } from './layouts';
import { Switch, Redirect, Route } from 'react-router-dom';

import {
  LoginPage as LoginPageView,
  Dashboard as DashboardView,
  SettingsAccounts as AccountsView,
  Build as BuildView,
  DesiredVersions as DesiredVersionsView,
  Logging as LoggingView,
  Faults as FaultsView,
  SettingsBuild as SettingsBuildView,
  SettingsProfiles as SettingsProfilesView,
  SettingsProviders as SettingsProvidersView,
  NotFound as NotFoundView
} from './views';
import {DistributionInfo, useDistributionInfoQuery} from './generated/graphql';

// @ts-ignore
export const LoginSwitchRoute = ({ component: Component, ...rest }) => (
  <Route {...rest} render = { props => {
    return localStorage.getItem('accessToken')
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
        component={BuildView}
        layout={MainLayout}
        path='/build'
        {...props}
      />
      <RouteWithLayout
        component={DesiredVersionsView}
        layout={MainLayout}
        path='/desiredVersions'
        {...props}
      />
      <RouteWithLayout
        component={LoggingView}
        layout={MainLayout}
        path='/logging'
        {...props}
      />
      <RouteWithLayout
        component={FaultsView}
        layout={MainLayout}
        path='/faults'
        exact
        {...props}
      />
      <RouteWithLayout
        component={AccountsView}
        layout={MainLayout}
        path='/settings/accounts'
        {...props}
      />
      <RouteWithLayout
        component={SettingsBuildView}
        layout={MainLayout}
        path='/settings/build'
        {...props}
      />
      <RouteWithLayout
        component={SettingsProfilesView}
        layout={MainLayout}
        path='/settings/profiles'
        {...props}
      />
      <RouteWithLayout
        component={SettingsProvidersView}
        layout={MainLayout}
        path='/settings/providers'
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
