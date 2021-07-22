import React from 'react';

import { makeStyles } from '@material-ui/core/styles';
import {Grid} from '@material-ui/core';
import {Route, Switch, useRouteMatch} from "react-router-dom";
import BuildDeveloper from "./components/Developer/BuildDeveloper";
import StartBuildService from "./components/Developer/StartBuildService";
import MonitorBuildService from "./components/Developer/MonitorBuildService";
import BuildClient from "./components/Client/BuildClient";

const useStyles = makeStyles(theme => ({
  root: {
    padding: theme.spacing(2)
  }
}));

const Build = () => {
  const classes = useStyles();
  const routeMatch = useRouteMatch();

  return (
    <div className={classes.root}>
      <Grid
        container
      >
        <Grid
          item
          xs={12}
        >
          <Switch>
            <Route exact path={`${routeMatch.url}/developer`}
                   component={BuildDeveloper}/>
            <Route exact path={`${routeMatch.url}/developer/start/:service`}
                   render={(props) => <StartBuildService fromUrl={routeMatch.url + '/developer'} {...props} /> }>
            </Route>
            <Route exact path={`${routeMatch.url}/developer/monitor/:service`}
                   render={(props) => <MonitorBuildService fromUrl={routeMatch.url + '/developer'} {...props} /> }>
            </Route>
            <Route exact path={`${routeMatch.url}/client`}
                   component={BuildClient}/>
          </Switch>
        </Grid>
      </Grid>
    </div>
  );
};

export default Build;
