import React from 'react';

import { makeStyles } from '@material-ui/core/styles';
import {Grid} from '@material-ui/core';
import ConsumersManager from "./components/Distribution/ConsumersManager";
import {Route, Switch, useRouteMatch} from "react-router-dom";
import ProvidersManager from "./components/Distribution/ProvidersManager";

const useStyles = makeStyles(theme => ({
  root: {
    padding: theme.spacing(2)
  }
}));

const Distribution = () => {
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
            <Route exact path={`${routeMatch.url}/provider`}
              component={ProvidersManager}/>
            <Route exact path={`${routeMatch.url}/consumers`}
              component={ConsumersManager}/>
          </Switch>
        </Grid>
      </Grid>
    </div>
  );
};

export default Distribution;
