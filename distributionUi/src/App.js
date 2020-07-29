import React from 'react';
import { BrowserRouter as Router, Route } from 'react-router-dom';
import { Redirect } from 'react-router-dom';

import { Desktop } from "./desktop";
import { LoginPage } from "./login";

import './App.css';

export const LoginSwitchRoute = ({ component: Component, ...rest }) => (
  <Route {...rest} render = { props => (
    localStorage.getItem('user')
        ? <Component {...props} />
        : <Redirect to={{ pathname: '/login', state: { from: props.location } }} />
  )} />
)

function App() {
  return (
    <div className="container">
        <div className="col-sm-8 col-sm-offset-2">
          <Router>
            <div>
              <Route exact path="/login" component={LoginPage} />
              <LoginSwitchRoute path="/" component={Desktop} />
            </div>
          </Router>
        </div>
    </div>
  );
}

export default App;