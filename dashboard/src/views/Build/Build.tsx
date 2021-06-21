import React from 'react';

import { makeStyles } from '@material-ui/core/styles';
import {Grid} from '@material-ui/core';
import {Route, Switch, useRouteMatch} from "react-router-dom";
import BuildDeveloper from "./components/Developer/BuildDeveloper";
import BuildService from "./components/Developer/BuildService";

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
            <Route exact path={`${routeMatch.url}/developer/:service`}
                   render={(props) => <BuildService fromUrl={routeMatch.url} {...props} /> }>
            </Route>
            <Route exact path={`${routeMatch.url}/client`} />
          </Switch>
        </Grid>
      </Grid>
    </div>
  );
};

export default Build;
