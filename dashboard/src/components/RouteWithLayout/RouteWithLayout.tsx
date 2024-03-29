import React from 'react';
import { Route } from 'react-router-dom';

interface RouteWithLayoutProps {
  layout: any,
  component: any,
  path: string,
  exact?: boolean
}

const RouteWithLayout: React.FC<RouteWithLayoutProps> = props => {
  const { layout: Layout, component: Component, ...rest } = props;

  return (
    <Route
      {...rest}
      render={matchProps => (
        <Layout>
          <Component {...matchProps} {...rest}/>
        </Layout>
      )}
    />
  );
};

export default RouteWithLayout;
