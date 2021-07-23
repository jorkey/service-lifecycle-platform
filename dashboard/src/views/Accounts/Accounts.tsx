import React from 'react';

import { makeStyles } from '@material-ui/core/styles';
import {Grid} from '@material-ui/core';
import AccountManager from "./components/Accounts/AccountManager";
import AccountEditor from "./components/Accounts/AccountEditor";
import {Route, RouteComponentProps, Switch, useRouteMatch} from "react-router-dom";

const useStyles = makeStyles(theme => ({
  root: {
    padding: theme.spacing(2)
  }
}));

const Accounts = () => {
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
            <Route exact path={`${routeMatch.url}/:type`}
              component={AccountManager}/>
            <Route exact path={`${routeMatch.url}/:type/new`}
              render={(props) => <AccountEditor fromUrl={routeMatch.url} {...props} /> }>
            </Route>
            <Route exact path={`${routeMatch.url}/:type/edit/:account`}
              render={(props) => <AccountEditor fromUrl={routeMatch.url} {...props} /> }>
            </Route>
          </Switch>
        </Grid>
      </Grid>
    </div>
  );
};

export default Accounts;
