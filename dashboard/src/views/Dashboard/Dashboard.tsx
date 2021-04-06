import React from 'react';
import { makeStyles } from '@material-ui/core/styles';
import { Grid } from '@material-ui/core';

import {
  Versions,
} from './components';
import {useDistributionInfoQuery} from "../../generated/graphql";
import {Utils} from "../../common";

const useStyles = makeStyles(theme => ({
  root: {
    padding: theme.spacing(2)
  }
}));

interface DashboardProps {
  distributionTitle: string
}

const Dashboard: React.FC<DashboardProps> = props => {
  const classes = useStyles();

  return (
    <div className={classes.root}>
      <Grid
        container
      >
        <Grid
          item
          xs={12}
        >
          <Versions distributionName={props.distributionTitle}/>
        </Grid>
      </Grid>
    </div>
  );
};

export default Dashboard;
