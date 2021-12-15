import React from 'react';

import { makeStyles } from '@material-ui/core/styles';
import {Grid} from '@material-ui/core';
import {Route, Switch, useRouteMatch} from "react-router-dom";
import DeveloperBuilderConfiguration from "./components/Developer/DeveloperBuilderConfiguration";
import DeveloperServiceEditor from "./components/Developer/DeveloperServiceEditor";

const useStyles = makeStyles(theme => ({
  root: {
    padding: theme.spacing(2)
  }
}));

const Builder = () => {
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
            <Route exact path={`${routeMatch.url}/developer/service/new`}
                   render={(props) =>
                     <DeveloperServiceEditor fromUrl={`${routeMatch.url}/developer`} {...props} />}>
            </Route>
            <Route exact path={`${routeMatch.url}/developer/edit/:service`}
                   render={(props) =>
                     <DeveloperServiceEditor fromUrl={`${routeMatch.url}/developer`} {...props} />}>
            </Route>
          </Switch>
        </Grid>
      </Grid>
    </div>
  );
};

export default Builder;
