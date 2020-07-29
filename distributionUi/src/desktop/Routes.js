import React from 'react';

import {Switch, Route} from 'react-router-dom';
import {Dashboard, Services, Clients, Logging, Failures} from "../views";

const Routes = () => {
  return (
    <Switch>
      <Route path="/dashboard" component={Dashboard} />
      <Route path="/services" component={Services} />
      <Route path="/clients" component={Clients} />
      <Route path="/logging" component={Logging} />
      <Route path="/failures" component={Failures} />
    </Switch>
  );
};

export default Routes;
