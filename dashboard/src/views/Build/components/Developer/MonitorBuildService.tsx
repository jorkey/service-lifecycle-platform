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

  const [initialized, setInitialized] = useState(false)
  const [terminated, setTerminated] = useState(false)

  const [error, setError] = useState<string>()

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
    } else {
      setError('Building of service is not in process now')
    }
    setInitialized(true)
  }

  const MonitorCard = () => {
    return (
      <Card className={classes.card}>
        <CardHeader title={`Building Service '${service}'`}/>
        <CardContent>
          <Grid container component='dl' spacing={3}>
            <Grid item md={2} xs={12}>
              <Typography component='dt' variant='h6'>Version</Typography>
            </Grid>
            <Grid item md={10} xs={12}>
              <Typography component='dd'>{version}</Typography>
            </Grid>
            <Grid item md={2} xs={12}>
              <Typography component='dt' variant='h6'>Comment</Typography>
            </Grid>
            <Grid item md={10} xs={12}>
              <Typography component='dd'>{comment}</Typography>
            </Grid>
          </Grid>
          <BranchesTable
            branches={sources?.map(source => { return { name: source.name, branch: source.git.branch } })}
            editable={false}
          />
          { <TaskLogs task={task} terminated={terminated} onTerminated={() => { setTerminated(true) }}/> }
      </CardContent>
    </Card>)
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
          { !terminated ?
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
