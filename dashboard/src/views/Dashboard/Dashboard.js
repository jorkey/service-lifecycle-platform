import React from 'react';
import { makeStyles } from '@material-ui/core/styles';
import { Grid } from '@material-ui/core';

import {
  Versions,
} from './components';

let classes = makeStyles(theme => ({
  root: {
    padding: theme.spacing(4)
  }
}));

const Dashboard = () => {
  return (
    <div className={classes.root}>
      <Grid
        container
        spacing={4}
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