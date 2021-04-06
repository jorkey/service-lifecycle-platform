import React from 'react';
import { makeStyles } from '@material-ui/styles';

import { Topbar } from './components';
import {DistributionInfo} from "../../generated/graphql";

const useStyles = makeStyles(() => ({
  root: {
    paddingTop: 64,
    height: '100%'
  },
  content: {
    height: '100%'
  }
}));

interface MinimalProps {
  distributionInfo: DistributionInfo,
  children: any
}

const Minimal: React.FC<MinimalProps> = props => {
  const { distributionInfo, children, ...rest } = props;

  const classes = useStyles();

  return (
    <div className={classes.root}>
      <Topbar distributionInfo={distributionInfo} {...rest}/>
      <main className={classes.content}>{children}</main>
    </div>
  );
};

export default Minimal;
