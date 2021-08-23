import React from 'react';

import { makeStyles } from '@material-ui/core/styles';
import {Grid} from '@material-ui/core';
import AccountManager from "./components/Accounts/AccountManager";
import {Route, RouteComponentProps, Switch, useRouteMatch} from "react-router-dom";
import UserEditor from "./components/Accounts/UserEditor";
import ServiceEditor from "./components/Accounts/ServiceEditor";
import ConsumerEditor from "./components/Accounts/ConsumerEditor";

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
            <Route exact path={[`${routeMatch.url}/users/new`, `${routeMatch.url}/users/edit/:account`]}
              render={(props) => <UserEditor fromUrl={routeMatch.url + '/users'} {...props} /> }>
            </Route>
            <Route exact path={[`${routeMatch.url}/services/new`, `${routeMatch.url}/services/edit/:account`]}
                   render={(props) => <ServiceEditor fromUrl={routeMatch.url + '/services'} {...props} /> }>
            </Route>
            <Route exact path={[`${routeMatch.url}/consumers/new`, `${routeMatch.url}/consumers/edit/:account`]}
                   render={(props) => <ConsumerEditor fromUrl={routeMatch.url + '/consumers'} {...props} /> }>
            </Route>
          </Switch>
        </Grid>
      </Grid>
    </div>
  );
};

export default Accounts;
