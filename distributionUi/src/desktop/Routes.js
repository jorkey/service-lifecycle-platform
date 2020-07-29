import React from 'react';

import {Switch, Route} from 'react-router-dom';
import {Services, Clients} from "../views";

const Routes = () => {
  return (
    <Switch>
      <Route path="/services" component={Services} />
      <Route path="/clients" component={Clients} />
    </Switch>
  );
};

export default Routes;
