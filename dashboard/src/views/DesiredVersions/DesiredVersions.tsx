import React from 'react';

import { makeStyles } from '@material-ui/core/styles';
import {Grid} from '@material-ui/core';
import {Route, Switch, useRouteMatch} from "react-router-dom";
import ClientDesiredVersions from "./components/Client/ClientDesiredVersions";
import DeveloperDesiredVersions from "./components/Developer/DeveloperDesiredVersions";

const useStyles = makeStyles(theme => ({
  root: {
    padding: theme.spacing(2)
  }
}));

const DesiredVersions = () => {
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
                   component={DeveloperDesiredVersions}/>
            <Route exact path={`${routeMatch.url}/client`}
                   component={ClientDesiredVersions}/>
          </Switch>
        </Grid>
      </Grid>
    </div>
  );
};

export default DesiredVersions;
