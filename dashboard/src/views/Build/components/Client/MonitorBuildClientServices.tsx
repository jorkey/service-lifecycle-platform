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
  useCancelTaskMutation, useTasksQuery
} from '../../../../generated/graphql';
import Alert from "@material-ui/lab/Alert";
import {LogsTable} from "../../../../common/components/logsTable/LogsTable";

const useStyles = makeStyles(theme => ({
  card: {
    marginTop: 10
  },
  logsTable: {
    height: 'calc(100vh - 450px)',
  },
  controls: {
    marginTop: 12,
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
  task: string
}

interface MonitorBuildServicesParams extends RouteComponentProps<MonitorBuildServicesRouteParams> {
  fromUrl: string
}

const MonitorBuildClientServices = (props: MonitorBuildServicesParams) => {
  const classes = useStyles()

  const task = props.match.params.task

  const [startTime, setStartTime] = useState<Date>()
  const [author, setAuthor] = useState<string>()
  const [versions, setVersions] = useState<string>()
  const [endTime, setEndTime] = useState<Date>()

  const [initialized, setInitialized] = useState(false)
  const [terminatedStatus, setTerminatedStatus] = useState<boolean>()

  const [error, setError] = useState<string>()

  enum Status { InProcess, Success, Error}
  const [status, setStatus] = useState<Status>()

  const { data: tasks } = useTasksQuery({
    fetchPolicy: 'no-cache', // base option no-cache does not work
    variables: { task: task },
    onError(err) { setError('Query client versions in process error ' + err.message) },
  })
  const [ cancelTask ] = useCancelTaskMutation({
    variables: { task: task! },
    onError(err) { setError('Cancel task error ' + err.message) },
  })

  if (!initialized && tasks?.tasks.length) {
    if (tasks.tasks.length) {
      const task = tasks.tasks[0]
      setStartTime(task.creationTime)
      setAuthor(task.parameters.find(p => p.name == 'author')?.value)
      setVersions(task.parameters.find(p => p.name == 'versions')?.value)
      setStatus(Status.InProcess)
    } else {
      setError(`Can't find task ${task}`)
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
            { versions?
              <Grid item md={11} xs={12}>
                <Typography>{ versions }</Typography>
              </Grid> : null }

            <Grid item md={1} xs={12}>
              <Typography>Author</Typography>
            </Grid>
            <Grid item md={2} xs={12}>
              <Typography>{author}</Typography>
            </Grid>

            <Grid item md={1} xs={12}>
              <Typography>Task</Typography>
            </Grid>
            <Grid item md={8} xs={12}>
              <Typography>{task}</Typography>
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
                  <Typography>{startTime.toLocaleTimeString()}</Typography>
                </Grid>
              </> : (startTime && endTime) ?
              <>
                <Grid item md={1} xs={12}>
                  <Typography>Start / End</Typography>
                </Grid>
                <Grid item md={8} xs={12}>
                  <Typography>{startTime.toLocaleTimeString() + ' / ' + endTime.toLocaleTimeString()}</Typography>
                </Grid>
              </> : null
            }
          </Grid>
        </CardContent>
      </Card>
      <Card className={classes.card}>
        <CardHeader title={`Task logs`}/>
        <CardContent>
          { <LogsTable className={classes.logsTable}
                       task={task!}
                       follow={true}
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
      <div>
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
      </div> : null
  )
}

export default MonitorBuildClientServices;
