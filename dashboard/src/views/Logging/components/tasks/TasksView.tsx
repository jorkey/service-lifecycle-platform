import React, {useCallback, useEffect, useRef, useState} from 'react';
import { makeStyles } from '@material-ui/styles';
import {
  Button,
  Card,
  CardContent, CardHeader, Checkbox, Grid, Select, TextField,
} from '@material-ui/core';
import Alert from "@material-ui/lab/Alert";
import FormGroup from "@material-ui/core/FormGroup";
import FormControlLabel from "@material-ui/core/FormControlLabel";
import {RouteComponentProps, useHistory} from "react-router-dom";
import {
  useLogDirectoriesLazyQuery,
  useLogInstancesLazyQuery, useLogLevelsQuery, useLogsStartTimeQuery, useLogsEndTimeQuery, useLogProcessesLazyQuery,
  useLogServicesQuery, useTasksQuery, useTaskTypesQuery
} from "../../../../generated/graphql";
import {DateTimePicker} from "@material-ui/pickers";
import {LogsTable, LogsTableEvents} from "../../../../common/components/logsTable/LogsTable";
import {TasksTable} from "../../../../common/components/tasksTable/TasksTable";

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
  taskTypeSelect: {
    marginLeft: '10px',
    width: '150px',
  },
  onlyActive: {
    marginLeft: '10px',
    width: '50px',
  },
  tasksTable: {
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

interface TasksRouteParams {
}

interface TasksParams extends RouteComponentProps<TasksRouteParams> {
  fromUrl: string
}

const TasksView: React.FC<TasksParams> = props => {
  const classes = useStyles()

  const [taskType, setTaskType] = useState<string>()
  const [onlyActive, setOnlyOnlyActive] = useState<boolean>()

  const history = useHistory()
  const [error, setError] = useState<string>()

  const { data: taskTypes } = useTaskTypesQuery({
    fetchPolicy: 'no-cache',
    onError(err) { setError('Query task types error ' + err.message) },
  })

  const handleOnClick = useCallback((task: string) => {
    history.push('tasks/' + task)
  }, [ history ]);

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
                          className={classes.taskTypeSelect}
                          native
                          onChange={(event) => {
                            setTaskType(event.target.value? event.target.value as string: undefined)
                          }}
                          title='Task Type'
                          value={taskType}
                        >
                          <option key={-1}/>
                          { taskTypes?.taskTypes
                              .map((type, index) => <option key={index}>{type}</option>)}
                        </Select>
                      }
                      label='Task Type'
                    />
                    <FormControlLabel
                      className={classes.control}
                      labelPlacement={'start'}
                      control={
                        <Checkbox
                          className={classes.onlyActive}
                          onChange={ event => setOnlyOnlyActive(event.target.checked) }
                          title='Only Active'
                          value={onlyActive}
                        />
                      }
                      label='Only Active'
                    />
                  </FormGroup>
                </>
              }
              title={'Tasks'}
            />
            <CardContent className={classes.content}>
              <div className={classes.inner}>
                <TasksTable className={classes.tasksTable}
                           type={taskType}
                           onlyActive={onlyActive}
                           onClick={(task) => { handleOnClick(task) }}
                           onError={error => {setError(error)}}
                />
                {error && <Alert className={classes.alert} severity="error">{error}</Alert>}
              </div>
            </CardContent>
          </Card>
        </Grid>
      </Grid>
    </div>
  )
}

export default TasksView