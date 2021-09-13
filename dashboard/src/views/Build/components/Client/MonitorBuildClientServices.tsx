import React, {useState} from 'react';

import Button from '@material-ui/core/Button';
import {NavLink as RouterLink, RouteComponentProps} from "react-router-dom"

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

interface MonitorBuildServicesRouteParams {
}

interface MonitorBuildServicesParams extends RouteComponentProps<MonitorBuildServicesRouteParams> {
  fromUrl: string
}

const MonitorBuildClientServices = (props: MonitorBuildServicesParams) => {
  const classes = useStyles()

  const [task, setTask] = useState<string>()
  const [versions, setVersions] = useState<DeveloperDesiredVersion[]>([])
  const [author, setAuthor] = useState<string>()
  const [startTime, setStartTime] = useState<Date>()
  const [endTime, setEndTime] = useState<Date>()

  const [initialized, setInitialized] = useState(false)
  const [terminatedStatus, setTerminatedStatus] = useState<boolean>()

  const [error, setError] = useState<string>()

  enum Status { InProcess, Success, Error}
  const [status, setStatus] = useState<Status>()

  const { data: versionsInProcess } = useClientVersionsInProcessQuery({
    fetchPolicy: 'no-cache',
    onError(err) { setError('Query client versions in process error ' + err.message) },
  })
  const [ cancelTask ] = useCancelTaskMutation({
    variables: { task: task! },
    fetchPolicy: 'no-cache',
    onError(err) { setError('Cancel task error ' + err.message) },
  })

  if (!initialized && versionsInProcess?.clientVersionsInProcess) {
    if (versionsInProcess.clientVersionsInProcess) {
      const v = versionsInProcess.clientVersionsInProcess
      setTask(v.task)
      setVersions(v.versions)
      setAuthor(v.author)
      setStartTime(v.startTime)
      setStatus(Status.InProcess)
    } else {
      setError(`Building of services is not in process now`)
    }
    setInitialized(true)
  }

  const MonitorCard = () => {
    return (<>
      <Card className={classes.card}>
        <CardHeader title={`Building Services`}/>
        <CardContent>
          <Grid container spacing={1}>
            <Grid item md={1} xs={12}>
              <Typography>Versions</Typography>
            </Grid>
            <Grid item md={11} xs={12}>
              { versions?.map((version, index) => (<Typography key={index}>{ version.service + ':' +
                  Version.developerDistributionVersionToString(version.version) }</Typography>)) }
            </Grid>

            <Grid item md={1} xs={12}>
              <Typography>Author</Typography>
            </Grid>
            <Grid item md={11} xs={12}>
              <Typography>{author}</Typography>
            </Grid>

            <Grid item md={1} xs={12}>
              <Typography>Status</Typography>
            </Grid>
            <Grid item md={11} xs={12}>
              <Typography>{status == Status.InProcess ? 'In Process': status == Status.Success ? 'Success' : 'Error'}</Typography>
            </Grid>

            { (startTime && !endTime) ?
              <>
                <Grid item md={1} xs={12}>
                  <Typography>Start</Typography>
                </Grid>
                <Grid item md={11} xs={12}>
                  <Typography>{startTime.toLocaleString()}</Typography>
                </Grid>
              </> : (startTime && endTime) ?
              <>
                <Grid item md={1} xs={12}>
                  <Typography>Start / End</Typography>
                </Grid>
                <Grid item md={11} xs={12}>
                  <Typography>{startTime.toLocaleString() + ' / ' + endTime.toLocaleString()}</Typography>
                </Grid>
              </> : null
            }
          </Grid>
        </CardContent>
      </Card>
      <Card className={classes.card}>
        <CardHeader title={`Task logs`}/>
        <CardContent>
          { <LogsTable task={task!}
                       subscribe={true}
                       onComplete={
                          (date, stat) => {
                            setStatus(stat ? Status.Success: Status.Error)
                            setEndTime(date)
                            setTerminatedStatus(stat)
                          }
                       }
                       onError={message => setError(message)}
          /> }
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

export default MonitorBuildClientServices;
