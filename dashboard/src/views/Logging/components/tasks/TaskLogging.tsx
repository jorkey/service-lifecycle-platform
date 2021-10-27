import React, {useEffect, useRef, useState} from 'react';
import { makeStyles } from '@material-ui/styles';
import {
  Button,
  Card,
  CardContent, CardHeader, Checkbox, Grid, Select, TextField,
} from '@material-ui/core';
import Alert from "@material-ui/lab/Alert";
import FormGroup from "@material-ui/core/FormGroup";
import FormControlLabel from "@material-ui/core/FormControlLabel";
import {RouteComponentProps} from "react-router-dom";
import {
  useLogDirectoriesLazyQuery,
  useLogInstancesLazyQuery, useLogLevelsQuery, useLogsStartTimeQuery, useLogsEndTimeQuery, useLogProcessesLazyQuery,
  useLogServicesQuery, useTasksQuery
} from "../../../../generated/graphql";
import {DateTimePicker} from "@material-ui/pickers";
import {LogsTable, LogsTableEvents} from "../../../../common/components/logsTable/LogsTable";
import {Logs} from "../../../../common/Logs";

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
  find: {
    marginLeft: '10px',
    width: '230px',
  },
  follow: {
    marginLeft: '10px',
    width: '50px',
  },
  top: {
    marginLeft: '10px',
    width: '50px',
    textTransform: 'none',
  },
  bottom: {
    marginLeft: '10px',
    width: '50px',
    textTransform: 'none',
  },
  level: {
    marginLeft: '10px',
    width: '100px',
  },
  logsTable: {
    height: 'calc(100vh - 250px)',
  },
  control: {
    paddingLeft: '5px',
    paddingRight: '15px',
    textTransform: 'none'
  },
  alert: {
    marginTop: 25
  }
}));

interface TaskLoggingRouteParams {
  task: string
}

interface TaskLoggingParams extends RouteComponentProps<TaskLoggingRouteParams> {
  fromUrl?: string
}

const TaskLogging: React.FC<TaskLoggingParams> = props => {
  const classes = useStyles()

  const [task] = useState(props.match.params.task)
  const [level, setLevel] = useState<string>()
  const [find, setFind] = useState<string>('')
  const [follow, setFollow] = useState<boolean>()

  const [error, setError] = useState<string>()

  const tableRef = useRef<LogsTableEvents>(null)

  const { data: activeTask } = useTasksQuery({
    variables: { id: task, onlyActive: true },
    fetchPolicy: 'no-cache',
    onError(err) { setError('Query active task error ' + err.message) },
  })

  const { data: levels } = useLogLevelsQuery({
    variables: { task:task },
    fetchPolicy: 'no-cache',
    onError(err) { setError('Query log levels error ' + err.message) },
  })

  const active = !!activeTask?.tasks.length

  return (
    <div className={classes.root}>
      <Grid container spacing={3}>
        <Grid item xs={12}>
          <Card>
            <CardHeader
              action={
                <>
                  <FormGroup row>
                    <FormControlLabel
                      className={classes.control}
                      labelPlacement={'start'}
                      control={
                        <Select
                          className={classes.level}
                          native
                          onChange={(event) => {
                            setLevel(event.target.value as string)
                          }}
                          title='Select level'
                          value={level}
                        >
                          <option key={-1}/>
                          { levels ? Logs.sortLevels(levels.logLevels)
                            .map((level, index) => <option key={index}>{level}</option>) : undefined }
                        </Select>
                      }
                      label='Level'
                    />
                    {!follow ? <FormControlLabel
                      className={classes.control}
                      labelPlacement={'start'}
                      control={
                        <TextField
                          className={classes.find}
                          onChange={(event) => {
                            setFind(event.target.value)
                          }}
                          title='Find Text'
                          value={find}
                        />
                      }
                      label='Find Text'
                    /> : null}
                    {!follow ? <Button
                      className={classes.top}
                      color="primary"
                      variant="contained"
                      onClick={() => tableRef.current?.toTop()}
                    >
                      Top
                    </Button> : null}
                    {!follow ? <Button
                      className={classes.bottom}
                      color="primary"
                      variant="contained"
                      onClick={() => tableRef.current?.toBottom()}
                    >
                      Bottom
                    </Button> : null}
                    {active?<FormControlLabel
                      className={classes.control}
                      labelPlacement={'start'}
                      control={
                        <Checkbox
                          className={classes.follow}
                          onChange={ event => setFollow(event.target.checked) }
                          title='Follow'
                          value={follow}
                        />
                      }
                      label='Follow'
                    />:null}
                  </FormGroup>
                </>
              }
              title={`Logs Of Task "${task}"`}
            />
            <CardContent className={classes.content}>
              <div className={classes.inner}>
                { levels ?
                  <LogsTable ref={tableRef}
                             className={classes.logsTable}
                             task={task}
                             levels={level?Logs.levelWithSubLevels(level, levels.logLevels):undefined}
                             find={find != ''?find:undefined}
                             follow={follow}
                             onComplete={() => {}}
                             onError={error => {setError(error)}}
                  /> : null }
                {error && <Alert className={classes.alert} severity="error">{error}</Alert>}
              </div>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </div>
  )
}

export default TaskLogging