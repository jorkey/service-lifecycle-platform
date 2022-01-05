import React from 'react';

import {Route, Switch, useRouteMatch} from "react-router-dom";
import ProvidersManager from "./components/Providers/ProvidersManager";

const Providers = () => {
  const routeMatch = useRouteMatch();

  return (
    <Switch>
      <Route exact path={`${routeMatch.url}`}
        component={ProvidersManager}/>
    </Switch>
  );
};

export default Providers;
