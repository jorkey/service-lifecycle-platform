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
import {ApolloLink, ApolloProvider, Operation, Resolvers, ServerError, split} from '@apollo/client';
import { ApolloClient, createHttpLink, InMemoryCache } from '@apollo/client';
import { setContext } from '@apollo/client/link/context';
import {onError} from '@apollo/client/link/error';
import DateFnsUtils from "@date-io/date-fns";
import {MuiPickersUtilsProvider} from "@material-ui/pickers";
import {withScalars} from "apollo-link-scalars";
import introspectionResult from "./generated/graphql.schema.json";
import {buildClientSchema, GraphQLScalarType, IntrospectionQuery, OperationDefinitionNode} from "graphql"
import {stripProperty} from "./common/Graphql";
import {NextLink} from "@apollo/client/link/core/types";
import {getMainDefinition, Observable} from '@apollo/client/utilities';
import EventSource from 'eventsource'
import { WebSocketLink } from "@apollo/client/link/ws"
import {SubscriptionClient} from "subscriptions-transport-ws";

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
        `GraphQL error: message: ${message}, location: ${locations}, path: ${path}`,
      ),
    )
  }

  if (networkError) {
    console.log(`Network error: ${JSON.stringify(networkError)}`);
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

const removeTypenameLink = new ApolloLink(
  (operation: Operation, forward: NextLink) => {
    operation.variables = stripProperty(operation.variables, '__typename');
    return forward(operation);
  }
)

const sseLink = new ApolloLink(
  (operation: Operation, forward: NextLink) => {
    const token = localStorage.getItem('token');
    return operation.operationName.startsWith('subscribe') ?
      new Observable(observer => {
        const source = new EventSource('/graphql'
          + '?query=' + operation.query.loc?.source.body.trim()
          + '&operation=' + operation.operationName
          + '&variables=' + JSON.stringify(operation.variables).trim(),
          { headers: { 'Authorization': `Bearer ${token}` }})

        source.onopen = (event) => {
          console.log('source.onopen ' + JSON.stringify(event))
        };
        source.onmessage = (event) => {
          console.log('source.onmessage ' + JSON.stringify(event))
          observer.next(event);
        };
        source.onerror = (exception) => {
          console.log('source.onerror ' + JSON.stringify(exception))
          observer.error(exception);
        };

        return () => {
          source.close();
        };
      })
      : forward(operation);
  }
)

const wsLink: ApolloLink = new WebSocketLink(new SubscriptionClient('ws://localhost:8000/graphql',{
    reconnect: true
}))

const link = ApolloLink.from([
  removeTypenameLink,
  authLink,
  errorLink,
  scalarsLink,
  split(
    ({ query }) => {
      const definition = getMainDefinition(query);
      return definition.kind === 'OperationDefinition' && definition.operation === 'subscription'
    },
    wsLink,
    httpLink
  )
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
