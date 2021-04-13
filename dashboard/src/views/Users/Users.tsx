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
  const match = useRouteMatch();

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
            <Route exact path={`${match.url}/users`}>
              <UsersManager/>
            </Route>
            <Route exact path={`${match.url}/users/:user`}
              component={UserEditor}>
            </Route>
          </Switch>
        </Grid>
      </Grid>
    </div>
  );
};

export default Users;
