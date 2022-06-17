import React, {useEffect, useRef, useState} from 'react';
import { makeStyles } from '@material-ui/styles';
import {
  Box,
  Button,
  Card,
  CardContent, CardHeader, Checkbox, Grid, Select, TextField,
} from '@material-ui/core';
import Alert from "@material-ui/lab/Alert";
import FormGroup from "@material-ui/core/FormGroup";
import FormControlLabel from "@material-ui/core/FormControlLabel";
import {NavLink as RouterLink, RouteComponentProps, useHistory} from "react-router-dom";
import {
  useLogLevelsQuery,
} from "../../../../generated/graphql";
import {LogsTable, LogsTableEvents} from "../../../../common/components/logsTable/LogsTable";
import {Logs} from "../../../../common/utils/Logs";

const useStyles = makeStyles((theme:any) => ({
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
    marginLeft: '30px',
    width: '50px',
  },
  bottom: {
    marginLeft: '10px',
    width: '50px',
  },
  level: {
    marginLeft: '10px',
    width: '100px',
  },
  logsTable: {
    height: 'calc(100vh - 250px)',
  },
  topControl: {
    paddingLeft: '5px',
    paddingRight: '15px',
  },
  alert: {
    marginTop: 25
  },
  controls: {
    marginRight: 16,
    display: 'flex',
    justifyContent: 'flex-end',
    p: 2
  },
  control: {
    marginLeft: '10px',
  },
}));

interface TaskLoggingRouteParams {
  task: string
}

interface TaskLoggingParams extends RouteComponentProps<TaskLoggingRouteParams> {
}

const TaskLogging: React.FC<TaskLoggingParams> = props => {
  const classes = useStyles()

  const [task] = useState(props.match.params.task)
  const [level, setLevel] = useState<string>()
  const [find, setFind] = useState<string>('')
  const [follow, setFollow] = useState<boolean>()

  const [error, setError] = useState<string>()

  const history = useHistory()

  const tableRef = useRef<LogsTableEvents>(null)

  const { data: levels } = useLogLevelsQuery({
    fetchPolicy: 'no-cache', // base option no-cache does not work
    variables: { task:task },
    onError(err) { setError('Query log levels error ' + err.message) },
  })

  return (<>
    <Card>
      <CardHeader
        action={
          <>
            <FormGroup row>
              <FormControlLabel
                className={classes.topControl}
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
                className={classes.topControl}
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
              <FormControlLabel
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
              />
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
    <Box className={classes.controls}>
      <Button
        className={classes.control}
        color="primary"
        variant="contained"
        onClick={() => history.goBack()}
      >
        Exit
      </Button>
    </Box>
  </>)
}

export default TaskLogging