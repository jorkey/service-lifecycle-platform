import React from 'react';

import { makeStyles } from '@material-ui/core/styles';
import {Grid} from '@material-ui/core';
import {Route, Switch, useRouteMatch} from "react-router-dom";
import DeveloperBuilderConfiguration from "./components/Developer/DeveloperBuilderConfiguration";
import DeveloperServiceEditor from "./components/Developer/DeveloperServiceEditor";
import ClientBuilderConfiguration from "./components/Client/ClientBuilderConfiguration";
import ClientServiceEditor from "./components/Client/ClientServiceEditor";

const Build = () => {
  const routeMatch = useRouteMatch();

  return (
    <Switch>
      <Route exact path={`${routeMatch.url}/developer`}
             component={DeveloperBuilderConfiguration}/>
      <Route exact path={`${routeMatch.url}/developer/new`}
             render={(props) =>
               <DeveloperServiceEditor fromUrl={`${routeMatch.url}/developer`} {...props} />}>
      </Route>
      <Route exact path={`${routeMatch.url}/developer/edit/:service`}
             render={(props) =>
               <DeveloperServiceEditor fromUrl={`${routeMatch.url}/developer`} {...props} />}>
      </Route>
      <Route exact path={`${routeMatch.url}/client`}
             component={ClientBuilderConfiguration}/>
      <Route exact path={`${routeMatch.url}/client/new`}
             render={(props) =>
               <ClientServiceEditor fromUrl={`${routeMatch.url}/client`} {...props} />}>
      </Route>
      <Route exact path={`${routeMatch.url}/client/edit/:service`}
             render={(props) =>
               <ClientServiceEditor fromUrl={`${routeMatch.url}/client`} {...props} />}>
      </Route>
    </Switch>
  );
};

export default Build;
