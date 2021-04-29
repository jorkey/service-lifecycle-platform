import React from 'react';

import { makeStyles } from '@material-ui/core/styles';
import {Grid} from '@material-ui/core';
import ProfilesManager from "./components/Profiles/ProfilesManager";
import ProfileEditor from "./components/Profiles/ProfileEditor";
import {Route, RouteComponentProps, Switch, useRouteMatch} from "react-router-dom";

const useStyles = makeStyles(theme => ({
  root: {
    padding: theme.spacing(2)
  }
}));

const Profiles = () => {
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
            <Route exact path={`${routeMatch.url}`}
              component={ProfilesManager}/>
            <Route exact path={`${routeMatch.url}/new`}
              render={(props) => <ProfileEditor fromUrl={routeMatch.url} {...props} /> }>
            </Route>
            <Route exact path={`${routeMatch.url}/edit/:profile`}
              render={(props) => <ProfileEditor fromUrl={routeMatch.url} {...props} /> }>
            </Route>
          </Switch>
        </Grid>
      </Grid>
    </div>
  );
};

export default Profiles;
