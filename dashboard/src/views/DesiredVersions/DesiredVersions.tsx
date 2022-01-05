import React from 'react';

import {Route, Switch, useRouteMatch} from "react-router-dom";
import ClientDesiredVersions from "./components/Client/ClientDesiredVersions";
import DeveloperDesiredVersions from "./components/Developer/DeveloperDesiredVersions";

const DesiredVersions = () => {
  const routeMatch = useRouteMatch();

  return (
    <Switch>
      <Route exact path={`${routeMatch.url}/developer`}
             component={DeveloperDesiredVersions}/>
      <Route exact path={`${routeMatch.url}/client`}
             component={ClientDesiredVersions}/>
    </Switch>
  );
};

export default DesiredVersions;
