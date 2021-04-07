import React from 'react';

import { makeStyles } from '@material-ui/core/styles';
import { Grid } from '@material-ui/core';
import UsersManager from "./components/Users/UsersManager";

const useStyles = makeStyles(theme => ({
  root: {
    padding: theme.spacing(2)
  }
}));

const Users = () => {
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
          <UsersManager/>
        </Grid>
      </Grid>
    </div>
  );
};

export default Users;
