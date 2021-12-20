import React from 'react';

import { makeStyles } from '@material-ui/core/styles';
import {Grid} from '@material-ui/core';
import {Route, Switch, useRouteMatch} from "react-router-dom";
import DeveloperBuilderConfiguration from "./components/Developer/DeveloperBuilderConfiguration";
import DeveloperServiceEditor from "./components/Developer/DeveloperServiceEditor";
import ClientBuilderConfiguration from "./components/Client/ClientBuilderConfiguration";
import ClientServiceEditor from "./components/Client/ClientServiceEditor";

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
        </Grid>
      </Grid>
    </div>
  );
};

export default Build;
