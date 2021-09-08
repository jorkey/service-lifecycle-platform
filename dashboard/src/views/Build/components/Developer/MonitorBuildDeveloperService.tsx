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
import {Version} from "../../../../common";
import {LogsTable} from "../../../../common/components/logsTable/LogsTable";

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

const MonitorBuildDeveloperService = (props: MonitorBuildServiceParams) => {
  const classes = useStyles()

  const service = props.match.params.service

  const [task, setTask] = useState<string>()
  const [version, setVersion] = useState<string>()
  const [author, setAuthor] = useState<string>()
  const [sources, setSources] = useState<SourceConfig[]>()
  const [comment, setComment] = useState<string>()
  const [startTime, setStartTime] = useState<Date>()
  const [endTime, setEndTime] = useState<Date>()

  const [initialized, setInitialized] = useState(false)
  const [terminatedStatus, setTerminatedStatus] = useState<boolean>()

  const [error, setError] = useState<string>()

  enum Status { InProcess, Success, Error}
  const [status, setStatus] = useState<Status>()

  const { data: versionInProcess } = useDeveloperVersionsInProcessQuery({
    variables: { service: service },
    fetchPolicy: 'no-cache',
    onError(err) { setError('Query developer versions in process error ' + err.message) },
  })
  const [ cancelTask ] = useCancelTaskMutation({
    variables: { task: task! },
    fetchPolicy: 'no-cache',
    onError(err) { setError('Cancel task error ' + err.message) },
  })

  if (!initialized && versionInProcess?.developerVersionsInProcess) {
    if (versionInProcess.developerVersionsInProcess.length) {
      const v = versionInProcess.developerVersionsInProcess![0]
      setTask(v.task)
      setVersion(Version.buildToString(v.version.build))
      setAuthor(v.author)
      setSources(v.sources)
      setComment(v.comment)
      setStartTime(v.startTime)
      setStatus(Status.InProcess)
    } else {
      setError(`Building of service ${service} is not in process now`)
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
              <Typography>{status == Status.InProcess ? 'In Process': status == Status.Success ? 'Success' : 'Error'}</Typography>
            </Grid>

            { (startTime && !endTime) ?
              <>
                <Grid item md={1} xs={12}>
                  <Typography>Start</Typography>
                </Grid>
                <Grid item md={8} xs={12}>
                  <Typography>{startTime.toLocaleString()}</Typography>
                </Grid>
              </> : (startTime && endTime) ?
              <>
                <Grid item md={1} xs={12}>
                  <Typography>Start / End</Typography>
                </Grid>
                <Grid item md={8} xs={12}>
                  <Typography>{startTime.toLocaleString() + ' / ' + endTime.toLocaleString()}</Typography>
                </Grid>
              </> :
              <Grid item md={9} xs={12}/>
            }
          </Grid>
        </CardContent>
      </Card>
      <Card className={classes.card}>
        <CardHeader title={`Task logs`}/>
        <CardContent>
          { <LogsTable params={ task= task! }
                       onComplete={
                        (time, stat) => {
                          setStatus(stat ? Status.Success: Status.Error)
                          setEndTime(time)
                          setTerminatedStatus(stat)
                        }
          }/> }
        </CardContent>
      </Card>
    </>)
  }

  return (
    initialized ?
      <Card
        className={clsx(classes.root)}
      >
        { task ?
          <>
            {MonitorCard()}
            <Divider/>
            {error && <Alert className={classes.alert} severity='error'>{error}</Alert>}
            <Box className={classes.controls}>
              { terminatedStatus == undefined ?
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
          </> : error ?
            <Alert className={classes.alert} severity='error'>{error}</Alert> : error
        }
      </Card> : null
  )
}

export default MonitorBuildDeveloperService;
