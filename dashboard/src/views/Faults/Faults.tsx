import React from 'react';

import {Route, Switch, useRouteMatch} from "react-router-dom";
import FaultsView from "./components/FaultsView";

const Faults = () => {
  const routeMatch = useRouteMatch();

  return (
    <Switch>
      <Route exact path={`${routeMatch.url}/`}
             component={FaultsView}/>
    </Switch>
  );
};

export default Faults;
