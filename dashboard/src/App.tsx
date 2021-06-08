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
import {ApolloLink, ApolloProvider, Resolvers, ServerError} from '@apollo/client';
import { ApolloClient, createHttpLink, InMemoryCache } from '@apollo/client';
import { setContext } from '@apollo/client/link/context';
import {onError} from '@apollo/client/link/error';
import DateFnsUtils from "@date-io/date-fns";
import {MuiPickersUtilsProvider} from "@material-ui/pickers";
import {withScalars} from "apollo-link-scalars";
import introspectionResult from "./generated/graphql.schema.json";
import {buildClientSchema, GraphQLScalarType, IntrospectionQuery} from "graphql"

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

const errorLink = onError(({ graphQLErrors, networkError: networkError}) => {
  if (graphQLErrors) {
    graphQLErrors.forEach(({message, locations, path}) =>
      console.log(
        `GraphQL error: Message: ${message}, Location: ${locations}, Path: ${path}`,
      ),
    )
  }

  if (networkError) {
    console.log(`Network error: ${networkError}`);
    if ((networkError as ServerError).statusCode === 401) {
      localStorage.removeItem('token')
      window.location.replace('/')
    }
  }
});

const schema = buildClientSchema((introspectionResult as unknown) as IntrospectionQuery)

const dateScalar = new GraphQLScalarType({
  name: 'Date',
  description: 'Date custom scalar type',
  serialize(value) {
    return value.getTime();
  },
  parseValue(value) {
    return new Date(value);
  }
});

const scalarsLink = withScalars({
  schema,
  typesMap: {
    Date: dateScalar
  }
});

const link = ApolloLink.from([
  errorLink,
  authLink,
  scalarsLink,
  httpLink
]);

const client = new ApolloClient({
  link: link,
  cache: new InMemoryCache()
});

export default class App extends Component {
  render() {
    return (
      <ApolloProvider client={client}>
        <MuiPickersUtilsProvider utils={DateFnsUtils}>
          <ThemeProvider theme={theme}>
            <Router history={browserHistory}>
              <LoginRoutes />
            </Router>
          </ThemeProvider>
        </MuiPickersUtilsProvider>
      </ApolloProvider>
    );
  }
}
