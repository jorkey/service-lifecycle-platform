import React from 'react';
import { BrowserRouter as Router, Route } from 'react-router-dom';

import { Redirect, Switch } from 'react-router-dom';

import { Desktop } from "./desktop";
import { LoginPage } from "./login";

import './App.css';

export const LoginSwitchRoute = ({ component: Component, ...rest }) => (
  <Route {...rest} render = { props => (
    localStorage.getItem('user')
        ? <Component {...props} />
        : <Redirect to="/login" />
  )} />
)

function App() {
  return (
    <div className="container">
        <div className="col-sm-8 col-sm-offset-2">
          <Router>
            <Switch>
              <Route exact path="/login" component={LoginPage} />
              <LoginSwitchRoute path="/" component={Desktop} />
            </Switch>
          </Router>
        </div>
    </div>
  );
}

export default App;
