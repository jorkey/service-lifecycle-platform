import React from 'react';

import {Grid} from '@material-ui/core';
import {Route, Switch, useRouteMatch} from "react-router-dom";
import FaultsView from "./components/FaultsView";

const Faults = () => {
  const routeMatch = useRouteMatch();

  return (
    <div>
      <Grid
        container
      >
        <Grid
          item
          xs={12}
        >
          <Switch>
            <Route exact path={`${routeMatch.url}/`}
                   component={FaultsView}/>
          </Switch>
        </Grid>
      </Grid>
    </div>
  );
};

export default Faults;
