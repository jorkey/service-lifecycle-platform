import React from 'react';

import { makeStyles } from '@material-ui/core/styles';
import {Grid} from '@material-ui/core';
import AccountManager from "./components/Accounts/AccountManager";
import {Route, RouteComponentProps, Switch, useRouteMatch} from "react-router-dom";
import UserEditor from "./components/Accounts/UserEditor";
import ServiceEditor from "./components/Accounts/ServiceEditor";
import ConsumerEditor from "./components/Accounts/ConsumerEditor";

const Accounts = () => {
  const routeMatch = useRouteMatch();

  return (
    <Switch>
      <Route exact path={`${routeMatch.url}/:type`}
        component={AccountManager}/>
      <Route exact path={[`${routeMatch.url}/users/new`, `${routeMatch.url}/users/edit/:account`]}
        render={(props) => <UserEditor {...props} /> }>
      </Route>
      <Route exact path={[`${routeMatch.url}/services/new`, `${routeMatch.url}/services/edit/:account`]}
             render={(props) => <ServiceEditor {...props} /> }>
      </Route>
      <Route exact path={[`${routeMatch.url}/consumers/new`, `${routeMatch.url}/consumers/edit/:account`]}
             render={(props) => <ConsumerEditor {...props} /> }>
      </Route>
    </Switch>
  );
};

export default Accounts;
