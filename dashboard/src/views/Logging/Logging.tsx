import React from 'react';

import { makeStyles } from '@material-ui/core/styles';
import {Grid} from '@material-ui/core';
import {Route, Switch, useRouteMatch} from "react-router-dom";
import TasksView from "./components/TasksView";
import LoggingView from "./components/LoggingView";

const useStyles = makeStyles(theme => ({
  root: {
    padding: theme.spacing(2)
  }
}));

const Logging = () => {
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
            <Route exact path={`${routeMatch.url}/tasks`}
                   component={TasksView}/>
            <Route exact path={`${routeMatch.url}/services`}
                   component={LoggingView}/>
          </Switch>
        </Grid>
      </Grid>
    </div>
  );
};

export default Logging;
