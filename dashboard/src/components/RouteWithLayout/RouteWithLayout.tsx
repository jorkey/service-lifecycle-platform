import React from 'react';
import { Route } from 'react-router-dom';
import { DistributionInfo } from '../../generated/graphql';

interface RouteWithLayoutProps {
  layout: any,
  component: any,
  path: string,
  exact: boolean,
  distributionInfo: DistributionInfo
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
