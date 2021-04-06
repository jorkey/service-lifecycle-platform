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
  children: any
}

const Minimal: React.FC<MinimalProps> = props => {
  const { children, ...rest } = props;

  const classes = useStyles();

  return (
    <div className={classes.root}>
      <Topbar {...rest}/>
      <main className={classes.content}>{children}</main>
    </div>
  );
};

export default Minimal;
