import React from 'react';

import { makeStyles } from '@material-ui/core/styles';
import {Grid} from '@material-ui/core';
import {Route, Switch, useRouteMatch} from "react-router-dom";
import BuildDeveloper from "./components/Developer/BuildDeveloper";
import StartBuildDeveloperService from "./components/Developer/StartBuildDeveloperService";
import MonitorBuildDeveloperService from "./components/Developer/MonitorBuildDeveloperService";
import StartBuildClientServices from "./components/Client/StartBuildClientServices";
import BuildClient from "./components/Client/BuildClient";
import MonitorBuildClientServices from "./components/Client/MonitorBuildClientServices";

const Build = () => {
  const routeMatch = useRouteMatch();

  return (<Switch>
      <Route exact path={`${routeMatch.url}/developer`}
             component={BuildDeveloper}/>
      <Route exact path={`${routeMatch.url}/developer/start/:service`}
             render={(props) => <StartBuildDeveloperService {...props} /> }>
      </Route>
      <Route exact path={`${routeMatch.url}/developer/monitor/:task`}
             render={(props) => <MonitorBuildDeveloperService {...props} /> }>
      </Route>

      <Route exact path={`${routeMatch.url}/client`}
             component={BuildClient}/>
      <Route exact path={`${routeMatch.url}/client/start`}
             render={(props) => <StartBuildClientServices {...props} /> }>
      </Route>
      <Route exact path={`${routeMatch.url}/client/monitor/:task`}
             render={(props) => <MonitorBuildClientServices {...props} /> }>
      </Route>
    </Switch>
  );
};

export default Build;
