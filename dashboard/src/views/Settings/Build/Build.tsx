import React from 'react';

import { makeStyles } from '@material-ui/core/styles';
import {Grid} from '@material-ui/core';
import {Route, Switch, useRouteMatch} from "react-router-dom";
import DeveloperBuildConfiguration from "./components/Developer/DeveloperBuildConfiguration";
import DeveloperBuildSettings from "./components/Developer/DeveloperBuildSettings";
import ClientBuildConfiguration from "./components/Client/ClientBuildConfiguration";
import ClientBuildSettings from "./components/Client/ClientBuildSettings";

const Build = () => {
  const routeMatch = useRouteMatch();

  return (
    <Switch>
      <Route exact path={`${routeMatch.url}/developer`}
             component={DeveloperBuildConfiguration}/>
      <Route exact path={`${routeMatch.url}/developer/new`}
             render={(props) =>
               <DeveloperBuildSettings new={true} fromUrl={`${routeMatch.url}/developer`} {...props} />}>
      </Route>
      <Route exact path={`${routeMatch.url}/developer/edit`}
             render={(props) =>
               <DeveloperBuildSettings fromUrl={`${routeMatch.url}/developer`} {...props} />}>
      </Route>
      <Route exact path={`${routeMatch.url}/developer/edit/:service`}
             render={(props) =>
               <DeveloperBuildSettings fromUrl={`${routeMatch.url}/developer`} {...props} />}>
      </Route>
      <Route exact path={`${routeMatch.url}/client`}
             component={ClientBuildConfiguration}/>
      <Route exact path={`${routeMatch.url}/client/new`}
             render={(props) =>
               <ClientBuildSettings new={true} fromUrl={`${routeMatch.url}/client`} {...props} />}>
      </Route>
      <Route exact path={`${routeMatch.url}/client/edit`}
             render={(props) =>
               <ClientBuildSettings fromUrl={`${routeMatch.url}/client`} {...props} />}>
      </Route>
      <Route exact path={`${routeMatch.url}/client/edit/:service`}
             render={(props) =>
               <ClientBuildSettings fromUrl={`${routeMatch.url}/client`} {...props} />}>
      </Route>
    </Switch>
  );
};

export default Build;
