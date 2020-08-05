import React from 'react';
import { makeStyles } from '@material-ui/core/styles';
import { Grid } from '@material-ui/core';

import {
  Versions,
} from './components';

const useStyles = makeStyles(theme => ({
  root: {
    padding: theme.spacing(2)
  }
}));

const Dashboard = () => {
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
          <Versions />
        </Grid>
      </Grid>
    </div>
  );
};

export default Dashboard;
