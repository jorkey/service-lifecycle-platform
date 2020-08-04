import React from 'react';
import { BrowserRouter as Router, Route } from 'react-router-dom';

import { Redirect, Switch } from 'react-router-dom';

import { Desktop } from "./desktop";
import { LoginPage } from "./login";

import { ThemeProvider } from '@material-ui/styles';

import theme from './theme';

export const LoginSwitchRoute = ({ component: Component, ...rest }) => (
  <Route {...rest} render = { props => (
    localStorage.getItem('user')
        ? <Component {...props} />
        : <Redirect to="/login" />
  )} />
)

function App() {
  return (
    <ThemeProvider theme={theme}>
      <Router>
        <Switch>
          <Route exact path="/login" component={LoginPage} />
          <LoginSwitchRoute path="/" component={Desktop} />
        </Switch>
      </Router>
    </ThemeProvider>
  );
}

export default App;
