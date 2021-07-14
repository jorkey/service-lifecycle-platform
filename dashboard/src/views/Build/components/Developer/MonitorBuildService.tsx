import React, {useEffect, useState} from 'react';

import Button from '@material-ui/core/Button';
import TextField from '@material-ui/core/TextField';
import {NavLink as RouterLink, RouteComponentProps, useRouteMatch, useHistory} from "react-router-dom"

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
  SourceConfig,
  useCancelTaskMutation,
  useDeveloperVersionsInProcessQuery,
} from '../../../../generated/graphql';
import clsx from 'clsx';
import Alert from "@material-ui/lab/Alert";
import BranchesTable from "./BranchesTable";
import {Version} from "../../../../common";
import {TaskLogs} from "../../../../common/components/logsTable/TaskLogs";

const useStyles = makeStyles(theme => ({
  root: {
    padding: theme.spacing(4)
  },
  card: {
    marginTop: 25
  },
  controls: {
    marginTop: 25,
    display: 'flex',
    justifyContent: 'flex-end',
    p: 2
  },
  control: {
    marginLeft: '25px',
    textTransform: 'none'
  },
  alert: {
    marginTop: 25
  }
}));

interface MonitorBuildServiceRouteParams {
  service: string
}

interface MonitorBuildServiceParams extends RouteComponentProps<MonitorBuildServiceRouteParams> {
  fromUrl: string
}

const MonitorBuildService = (props: MonitorBuildServiceParams) => {
  const classes = useStyles()

  const service = props.match.params.service

  const [task, setTask] = useState('')
  const [version, setVersion] = useState('')
  const [author, setAuthor] = useState('')
  const [sources, setSources] = useState<SourceConfig[]>([])
  const [comment, setComment] = useState('')
  const [startTime, setStartTime] = useState<Date>()

  const [initialized, setInitialized] = useState(false)
  const [terminatedStatus, setTerminatedStatus] = useState<boolean>()

  const [error, setError] = useState<string>()

  const [status, setStatus] = useState('')

  const { data: versionInProcess } = useDeveloperVersionsInProcessQuery({
    variables: { service: service },
    fetchPolicy: 'no-cache',
    onError(err) { setError('Query developer versions in process error ' + err.message) },
    onCompleted() { setError(undefined) }
  })
  const [ cancelTask ] = useCancelTaskMutation({
    variables: { task: task },
    fetchPolicy: 'no-cache',
    onError(err) { setError('Cancel task error ' + err.message) },
  })

  if (!initialized && versionInProcess) {
    if (versionInProcess.developerVersionsInProcess?.length) {
      const v = versionInProcess?.developerVersionsInProcess![0]
      setTask(v.task)
      setVersion(Version.buildToString(v.version.build))
      setAuthor(v.author)
      setSources(v.sources)
      setComment(v.comment)
      setStartTime(v.startTime)
      setStatus('In process')
    } else {
      setError('Building of service is not in process now')
    }
    setInitialized(true)
  }

  const MonitorCard = () => {
    return (<>
      <Card className={classes.card}>
        <CardHeader title={`Building Service '${service}'`}/>
        <CardContent>
          <Grid container spacing={1}>
            <Grid item md={1} xs={12}>
              <Typography>Version</Typography>
            </Grid>
            <Grid item md={2} xs={12}>
              <Typography>{version}</Typography>
            </Grid>

            <Grid item md={1} xs={12}>
              <Typography>Comment</Typography>
            </Grid>
            <Grid item md={8} xs={12}>
              <Typography>{comment}</Typography>
            </Grid>

            <Grid item md={1} xs={12}>
              <Typography>Author</Typography>
            </Grid>
            <Grid item md={2} xs={12}>
              <Typography>{author}</Typography>
            </Grid>

            <Grid item md={1} xs={12}>
              <Typography>Branches</Typography>
            </Grid>
            <Grid item md={8} xs={12}>
              <Typography>{sources?.map(source => { return source.name + ':' + source.git.branch })}</Typography>
            </Grid>

            <Grid item md={1} xs={12}>
              <Typography>Status</Typography>
            </Grid>
            <Grid item md={2} xs={12}>
              <Typography>{status}</Typography>
            </Grid>

            <Grid item md={1} xs={12}>
              <Typography>Start</Typography>
            </Grid>
            <Grid item md={8} xs={12}>
              <Typography>{startTime?.toLocaleString()}</Typography>
            </Grid>
          </Grid>
        </CardContent>
      </Card>
      <Card className={classes.card}>
        <CardHeader title={`Task logs`}/>
        <CardContent>
          { <TaskLogs task={task} terminated={terminatedStatus != undefined} onTerminated={
            stat => { setStatus(stat ? 'Success': 'Error'); setTerminatedStatus(true) }
          }/> }
        </CardContent>
      </Card>
    </>)
  }

  return (
    initialized ? (
      <Card
        className={clsx(classes.root)}
      >
        {MonitorCard()}
        <Divider />
        {error && <Alert className={classes.alert} severity='error'>{error}</Alert>}
        <Box className={classes.controls}>
          { !terminatedStatus ?
          <Button className={classes.control}
                  color="primary"
                  variant="contained"
                  onClick={ () => cancelTask() }
          >
            Stop Build
          </Button> : null }
          <Button className={classes.control}
                  color="primary"
                  variant="contained"
                  component={RouterLink}
                  to={ props.fromUrl }
          >
            Exit
          </Button>
        </Box>
      </Card>) : null
  );
}

export default MonitorBuildService;
