import React, {useCallback, useState} from 'react';
import { makeStyles } from '@material-ui/styles';
import {
  Card,
  CardContent, CardHeader, Checkbox, Select
} from '@material-ui/core';
import Alert from "@material-ui/lab/Alert";
import FormGroup from "@material-ui/core/FormGroup";
import FormControlLabel from "@material-ui/core/FormControlLabel";
import {RouteComponentProps, useHistory} from "react-router-dom";
import {
  useTaskServicesQuery,
  useTaskTypesQuery
} from "../../../../generated/graphql";
import {TasksTable} from "../../../../common/components/tasksTable/TasksTable";
import {DateTimePicker} from "@material-ui/pickers";

const useStyles = makeStyles((theme:any) => ({
  inner: {
    minWidth: 800
  },
  taskTypeSelect: {
    marginLeft: '10px',
    width: '200px',
  },
  taskServiceSelect: {
    marginLeft: '10px',
    width: '150px',
  },
  onlyActive: {
    marginLeft: '10px',
    width: '50px',
  },
  tasksTable: {
    height: 'calc(100vh - 200px)',
  },
  date: {
    marginLeft: '10px',
    width: '200px'
  },
  control: {
    paddingLeft: '5px',
    paddingRight: '15px',
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
  const [taskService, setTaskService] = useState<string>()
  const [onlyActive, setOnlyOnlyActive] = useState<boolean>()
  const [fromTime, setFromTime] = useState<Date>()
  const [toTime, setToTime] = useState<Date>()

  const history = useHistory()
  const [error, setError] = useState<string>()

  const { data: taskTypes } = useTaskTypesQuery({
    fetchPolicy: 'no-cache', // base option no-cache does not work
    onError(err) { setError('Query task types error ' + err.message) },
  })

  const { data: taskServices } = useTaskServicesQuery({
    fetchPolicy: 'no-cache', // base option no-cache does not work
    onError(err) { setError('Query task services error ' + err.message) },
  })

  const handleOnClick = useCallback((task: string) => {
    history.push('tasks/' + task)
  }, [ history ]);

  return (
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
                  <Select
                    className={classes.taskServiceSelect}
                    native
                    onChange={(event) => {
                      setTaskService(event.target.value? event.target.value as string: undefined)
                    }}
                    title='Service'
                    value={taskService}
                  >
                    <option key={-1}/>
                    { taskServices?.taskServices
                      .map((type, index) => <option key={index}>{type}</option>) }
                  </Select>
                }
                label='Service'
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
              <FormControlLabel
                className={classes.control}
                labelPlacement={'start'}
                control={
                  <DateTimePicker
                    className={classes.date}
                    value={fromTime?fromTime:null}
                    ampm={false}
                    onChange={(newValue) => {
                      setFromTime(newValue?newValue:undefined)
                    }}
                  />
                }
                label='From'
              />
              <FormControlLabel
                className={classes.control}
                labelPlacement={'start'}
                control={
                  <DateTimePicker
                    className={classes.date}
                    value={toTime?toTime:null}
                    ampm={false}
                    onChange={(newValue) => {
                      setToTime(newValue?newValue:undefined)
                    }}
                  />
                }
                label='To'
              />
            </FormGroup>
          </>
        }
        title={'Tasks'}
      />
      <CardContent>
        <div className={classes.inner}>
          <TasksTable className={classes.tasksTable}
                     type={taskType}
                     service={taskService}
                     onlyActive={onlyActive}
                     fromTime={fromTime}
                     toTime={toTime}
                     onClick={(task) => { handleOnClick(task) }}
                     onError={error => {setError(error)}}
          />
          {error && <Alert className={classes.alert} severity="error">{error}</Alert>}
        </div>
      </CardContent>
    </Card>
  )
}

export default TasksView