import React, {useState} from 'react';

import Button from '@material-ui/core/Button';
import {NavLink as RouterLink, RouteComponentProps, useHistory} from "react-router-dom"

import { makeStyles } from '@material-ui/core/styles';
import {
  Box,
  Card,
  CardContent,
  CardHeader,
  Divider,
  Grid, Typography
} from '@material-ui/core';
import {
  DeveloperDesiredVersion,
  useCancelTaskMutation, useClientVersionsInProcessQuery,
} from '../../../../generated/graphql';
import clsx from 'clsx';
import Alert from "@material-ui/lab/Alert";
import {Version} from "../../../../common";
import {TaskLogs} from "../../../../common/components/logsTable/TaskLogs";
import MonitorBuildClientServices from "./MonitorBuildServices";
import StartBuildClientServices from "./StartBuildServices";

const useStyles = makeStyles(theme => ({
  root: {
    padding: theme.spacing(4)
  },
  alert: {
    marginTop: 25
  }
}));

interface BuildClientRouteParams {
}

interface BuildClientParams extends RouteComponentProps<BuildClientRouteParams> {
  fromUrl: string
}

const BuildClient = (props: BuildClientParams) => {
  const classes = useStyles()

  const [error, setError] = useState<string>()
  const history = useHistory()

  const { data } = useClientVersionsInProcessQuery({
    fetchPolicy: 'no-cache',
    onCompleted(clientVersionsInProcess) {
      clientVersionsInProcess.clientVersionsInProcess?history.push('client/monitor/'):history.push('client/start')
    },
    onError(err) { setError('Query client versions in process error ' + err.message) },
  })

  return (
    error?<Alert className={classes.alert} severity="error">{error}</Alert>:null
  )
}

export default BuildClient;
