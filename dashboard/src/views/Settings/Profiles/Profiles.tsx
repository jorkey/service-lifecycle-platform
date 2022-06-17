import React from 'react';

import { makeStyles } from '@material-ui/core/styles';
import {Grid} from '@material-ui/core';
import {Route, Switch, useRouteMatch} from "react-router-dom";
import ProfilesManager from "./components/ProfilesManager";
import ProfileEditor from "./components/ProfileEditor";

const Profiles = () => {
  const routeMatch = useRouteMatch();

  return (
    <Switch>
      <Route exact path={routeMatch.url}
             component={ProfilesManager}/>
      <Route exact path={`${routeMatch.url}/new`}
             render={(props) => <ProfileEditor {...props} /> }>
      </Route>
      <Route exact path={`${routeMatch.url}/edit/:profile`}
             render={(props) => <ProfileEditor {...props} /> }>
      </Route>
    </Switch>
  );
};

export default Profiles;
