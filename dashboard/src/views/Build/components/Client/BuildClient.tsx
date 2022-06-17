import React, {useState} from 'react';

import {Redirect, RouteComponentProps, useHistory, useRouteMatch} from "react-router-dom"

import { makeStyles } from '@material-ui/core/styles';
import Alert from "@material-ui/lab/Alert";
import {useTasksQuery} from "../../../../generated/graphql";

const useStyles = makeStyles(theme => ({
  alert: {
    marginTop: 25
  }
}));

interface BuildClientRouteParams {
}

interface BuildClientParams extends RouteComponentProps<BuildClientRouteParams> {
}

const BuildClient = (props: BuildClientParams) => {
  const classes = useStyles()

  const history = useHistory()
  const routeMatch = useRouteMatch()
  const [error, setError] = useState<string>()

  useTasksQuery({
    fetchPolicy: 'no-cache', // base option no-cache does not work
    variables: { type: 'BuildClientVersions', onlyActive: true },
    onCompleted(tasks) {
      tasks.tasks.length?
        history.push(`${routeMatch.url}/monitor/` + tasks.tasks[0].task):
        history.push(`${routeMatch.url}/start`)
    },
    onError(err) { setError('Query client versions in process error ' + err.message) },
  })

  return (
    error?<Alert className={classes.alert} severity="error">{error}</Alert>:null
  )
}

export default BuildClient;
