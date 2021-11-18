import React from 'react';
import { makeStyles } from '@material-ui/core/styles';
import { Grid } from '@material-ui/core';
import Versions from "./components/Versions/Versions";
import LastClientVersions from "./components/LastClientVersions/LastClientVersions";
import {useDeveloperServicesQuery} from "../../generated/graphql";
import LastDeveloperVersions from "./components/LastDeveloperVersions/LastDeveloperVersions";

const useStyles = makeStyles(theme => ({
  root: {
    padding: theme.spacing(2)
  }
}));

interface DashboardProps {
}

const Dashboard: React.FC<DashboardProps> = props => {
  const classes = useStyles()

  const { data: developerServices } = useDeveloperServicesQuery()
  const development = !!developerServices?.developerServices?.length

  return (
    <div className={classes.root}>
      <Grid container spacing={3}>
        <Grid item xs={12}>
          <Versions/>
        </Grid>
        {development?<Grid item xs={12}>
          <LastDeveloperVersions/>
        </Grid>:null}
        <Grid item xs={12}>
          <LastClientVersions/>
        </Grid>
      </Grid>
    </div>
  );
};

export default Dashboard;
