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
import {ApolloLink, ApolloProvider, FetchResult, Operation, Resolvers, ServerError, split} from '@apollo/client';
import { ApolloClient, createHttpLink, InMemoryCache } from '@apollo/client';
import { setContext } from '@apollo/client/link/context';
import {onError} from '@apollo/client/link/error';
import DateFnsUtils from "@date-io/date-fns";
import {MuiPickersUtilsProvider} from "@material-ui/pickers";
import {withScalars} from "apollo-link-scalars";
import introspectionResult from "./generated/graphql.schema.json";
import {
  buildClientSchema,
  ExecutionResult,
  GraphQLError,
  GraphQLObjectType,
  GraphQLScalarType,
  IntrospectionQuery, Kind,
  print
} from "graphql"
import {stripProperty} from "./common/Graphql";
import {NextLink} from "@apollo/client/link/core/types";
import {getMainDefinition, Observable} from '@apollo/client/utilities';
import {Client, ClientOptions, createClient} from 'graphql-ws'
import BigInt from "apollo-type-bigint";
import Cookies from "universal-cookie";

const browserHistory = createBrowserHistory();

validate.validators = {
  ...validate.validators,
  ...validators
};

export const cookies = new Cookies()

const httpLink = createHttpLink({
  uri: '/graphql',
});

const authLink = setContext((_, { headers }) => {
  // get the authentication token from local storage if it exists
  const token = cookies.get('accessToken')
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
    console.log(`Network error: ${networkError.message}`);
    if ((networkError as ServerError).statusCode === 401) {
      localStorage.removeItem('accessToken')
      window.location.replace('/')
    }
  }
});

const schema = buildClientSchema((introspectionResult as unknown) as IntrospectionQuery)

const dateScalar = new GraphQLScalarType({
  name: 'Date',
  description: 'Date custom scalar type',
  serialize(value) {
    return value.toISOString()
  },
  parseValue(value) {
    return new Date(value)
  }
});

const bigIntScalar = new BigInt("bigInt")

const scalarsLink = withScalars({
  schema,
  typesMap: {
    Date: dateScalar,
    BigInt: bigIntScalar
  }
});

const removeTypenameLink = new ApolloLink(
  (operation: Operation, forward: NextLink) => {
    operation.variables = stripProperty(operation.variables, '__typename');
    return forward(operation);
  }
)

// const sseLink = new ApolloLink(
//   (operation: Operation, forward: NextLink) => {
//     const token = cookies.get('accessToken');
//     return operation.operationName.startsWith('subscribe') ?
//       new Observable(observer => {
//         const source = new EventSource('/graphql'
//           + '?query=' + operation.query.loc?.source.body.trim()
//           + '&operation=' + operation.operationName
//           + '&variables=' + JSON.stringify(operation.variables).trim(),
//           { headers: { 'Authorization': `Bearer ${token}` }})
//
//         source.onopen = (event) => {
//           console.log('source.onopen ' + JSON.stringify(event))
//         };
//         source.onmessage = (event) => {
//           console.log('source.onmessage ' + JSON.stringify(event))
//           observer.next(event);
//         };
//         source.onerror = (exception) => {
//           console.log('source.onerror ' + JSON.stringify(exception))
//           observer.error(exception);
//         };
//
//         return () => {
//           source.close();
//         };
//       })
//       : forward(operation);
//   }
// )

class WebSocketLink extends ApolloLink {
  private client: Client;

  constructor(options: ClientOptions) {
    super();
    this.client = createClient(options);
  }

  public request(operation: Operation): Observable<FetchResult> {
    return new Observable((sink) => {
      return this.client.subscribe<FetchResult>(
        { ...operation, query: print(operation.query) as string },
        {
          next: sink.next.bind(sink),
          complete: sink.complete.bind(sink),
          error: (err: any) => {
            if (err instanceof Error) {
              return sink.error(err);
            }

            if (err instanceof CloseEvent) {
              return sink.error(
                // reason will be available on clean closes
                new Error(
                  `Socket closed with event ${err.code} ${err.reason || ''}`,
                ),
              );
            }

            return sink.error(
              new Error(
                (err as GraphQLError[])
                  .map(({ message }) => message)
                  .join(', '),
              ),
            );
          },
        },
      );
    });
  }
}

const wsLink = new WebSocketLink({
  url: 'ws://localhost:8000/graphql/websocket',
  connectionParams: () => {
    const token = cookies.get('accessToken')
    return {
      Authorization: `Bearer ${token}`,
    };
  },
});

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
