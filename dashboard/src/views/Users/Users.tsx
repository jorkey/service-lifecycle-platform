import React from 'react';

import { makeStyles } from '@material-ui/core/styles';
import {Grid} from '@material-ui/core';
import UsersManager from "./components/Users/UsersManager";
import UserEditor from "./components/Users/UserEditor";
import {Route, Switch, useRouteMatch} from "react-router-dom";

const useStyles = makeStyles(theme => ({
  root: {
    padding: theme.spacing(2)
  }
}));

const Users = () => {
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
            <Route exact path={`${routeMatch.url}`}>
              <UsersManager/>
            </Route>
            <Route exact path={`${routeMatch.url}/new/:type`}
              render={(props) => <UserEditor fromUrl={routeMatch.url} {...props} /> }>
            </Route>
            <Route exact path={`${routeMatch.url}/edit/:user`}
              render={(props) => <UserEditor fromUrl={routeMatch.url} {...props} /> }>
            </Route>
          </Switch>
        </Grid>
      </Grid>
    </div>
  );
};

export default Users;
