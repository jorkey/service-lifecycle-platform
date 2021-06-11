import React from 'react';
import { makeStyles } from '@material-ui/core/styles';
import { Grid } from '@material-ui/core';
import LastVersions from "./components/LastVersions/LastVersions";
import VersionsInProcess from "./components/VersionsInProcess/VersionsInProcess";
import Versions from "./components/Versions/Versions";

const useStyles = makeStyles(theme => ({
  root: {
    padding: theme.spacing(2)
  }
}));

interface DashboardProps {
}

const Dashboard: React.FC<DashboardProps> = props => {
  const classes = useStyles();

  return (
    <div className={classes.root}>
      <Grid container spacing={3}>
        <Grid item xs={12}>
          <Versions/>
        </Grid>
        <Grid item xs={12}>
          <VersionsInProcess/>
        </Grid>
        <Grid item xs={12}>
          <LastVersions/>
        </Grid>
      </Grid>
    </div>
  );
};

export default Dashboard;
