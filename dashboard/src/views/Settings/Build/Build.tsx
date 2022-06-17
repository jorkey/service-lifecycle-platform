import React from 'react';

import {Route, Switch, useRouteMatch} from "react-router-dom";
import DeveloperBuildConfiguration from "./components/Developer/DeveloperBuildConfiguration";
import ClientBuildConfiguration from "./components/Client/ClientBuildConfiguration";
import DeveloperBuildSettings from "./components/Developer/DeveloperBuildSettings";
import ClientBuildSettings from "./components/Client/ClientBuildSettings";

const Build = () => {
  const routeMatch = useRouteMatch();

  return (
    <Switch>
      <Route exact path={`${routeMatch.url}/developer`}
             component={DeveloperBuildConfiguration}/>
      <Route exact path={`${routeMatch.url}/developer/new`}
             render={(props) =>
               <DeveloperBuildSettings new={true} {...props} />}>
      </Route>
      <Route exact path={`${routeMatch.url}/developer/edit`}
             render={(props) =>
               <DeveloperBuildSettings {...props} />}>
      </Route>
      <Route exact path={`${routeMatch.url}/developer/edit/:service`}
             render={(props) =>
               <DeveloperBuildSettings {...props} />}>
      </Route>
      <Route exact path={`${routeMatch.url}/client`}
             component={ClientBuildConfiguration}/>
      <Route exact path={`${routeMatch.url}/client/new`}
             render={(props) =>
               <ClientBuildSettings new={true} {...props} />}>
      </Route>
      <Route exact path={`${routeMatch.url}/client/edit`}
             render={(props) =>
               <ClientBuildSettings {...props} />}>
      </Route>
      <Route exact path={`${routeMatch.url}/client/edit/:service`}
             render={(props) =>
               <ClientBuildSettings {...props} />}>
      </Route>
    </Switch>
  );
};

export default Build;
