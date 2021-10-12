import React, {useEffect, useState} from 'react';
import { makeStyles } from '@material-ui/styles';
import {
  Card,
  CardContent, CardHeader, Grid, Select,
} from '@material-ui/core';
import FormGroup from "@material-ui/core/FormGroup";
import FormControlLabel from "@material-ui/core/FormControlLabel";
import {RouteComponentProps} from "react-router-dom";
import {
  DistributionFaultReport,
  useFaultDistributionsQuery, useFaultQuery, useFaultServicesLazyQuery, useFaultsQuery,
  useFaultsStartTimeQuery
} from "../../../generated/graphql";
import {DateTimePicker} from "@material-ui/pickers";
import Alert from "@material-ui/lab/Alert";
import {FaultsTable} from "./FaultsTable";
import TextField from "@material-ui/core/TextField";

const useStyles = makeStyles((theme:any) => ({
  root: {
    padding: theme.spacing(2)
  },
  content: {
    padding: 0
  },
  inner: {
    minWidth: 800
  },
}));

interface FaultParams {
  report: DistributionFaultReport
}

const FaultView: React.FC<FaultParams> = props => {
  const { report } = props

  const classes = useStyles()

  const info = `Fault Id: ${report.payload.id}\n` +
    `Instance: ${report.payload.info.instance}\n` +
    (report.payload.info.state.installTime?`Install Time: ${report.payload.info.state.installTime.toString()}\n`:'') +
    (report.payload.info.state.lastExitCode?`Last Exit Code: ${report.payload.info.state.lastExitCode}\n`:'') +
    (report.payload.info.state.failuresCount?`Failures Count: ${report.payload.info.state.failuresCount}\n`:'') +
    (report.payload.info.state.updateToVersion?`Updating To Version: ${report.payload.info.state.updateToVersion}\n`:'') +
    (report.payload.info.state.updateError?`Update Error: ${report.payload.info.state.updateError}\n`:'')
  return <div className={classes.root}>
    <Grid container spacing={3}>
      <Grid item xs={12}>
        <Card>
          <CardHeader title={'Fault Info'}/>
          <CardContent className={classes.content}>
            <div className={classes.inner}>
              <TextField>{info}</TextField>
            </div>
          </CardContent>
        </Card>
      </Grid>
    </Grid>
  </div>
}

export default FaultView