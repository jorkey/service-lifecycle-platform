import React, { Component } from 'react';
import { Router } from 'react-router-dom';
import { createBrowserHistory } from 'history';
import { ThemeProvider } from '@material-ui/styles';
import validate from 'validate.js';

import theme from './theme';
import 'react-perfect-scrollbar/dist/css/styles.css';
import './assets/scss/index.scss';
import validators from './common/validators';
import LoginRoutes from './Routes';
import {ApolloProvider} from "@apollo/client";
import { ApolloClient, createHttpLink, InMemoryCache } from '@apollo/client';
import { setContext } from '@apollo/client/link/context';
//import {Scalars} from "./graphql/scalars";

const browserHistory = createBrowserHistory();

validate.validators = {
  ...validate.validators,
  ...validators
};

const httpLink = createHttpLink({
  uri: '/graphql',
});

const authLink = setContext((_, { headers }) => {
  // get the authentication token from local storage if it exists
  const token = localStorage.getItem('token');
  // return the headers to the context so httpLink can read them
  if (token) {
    return {
      headers: {
        ...headers,
        authorization: `Bearer ${token}`
      }
    }
  }
  return headers
});

const resolvers = {
//  DeveloperVersion: Scalars.developerVersionScalar
};

const client = new ApolloClient({
  link: authLink.concat(httpLink),
  resolvers: resolvers,
  cache: new InMemoryCache()
});

export default class App extends Component {
  render() {
    return (
      <ApolloProvider client={client}>
        <ThemeProvider theme={theme}>
          <Router history={browserHistory}>
            <LoginRoutes />
          </Router>
        </ThemeProvider>
      </ApolloProvider>
    );
  }
}
