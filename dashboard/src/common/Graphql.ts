import { ApolloClient, InMemoryCache } from '@apollo/client';

function add1(a: number, b: number): number {
    return a + b;
}

let c = add1(1, 2)

class Graphql {
    client = new ApolloClient({
      uri: 'https://48p1r2roz4.sse.codesandbox.io',
      cache: new InMemoryCache()
    });

    add(a: number, b: number): number {
      return a + b;
    }
}

let graphql = new Graphql()
let l = graphql.add(1, 3)
