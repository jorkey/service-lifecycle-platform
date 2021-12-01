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
  useLogServicesQuery
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
  serviceSelect: {
    marginLeft: '10px',
    width: '150px',
  },
  instanceSelect: {
    marginLeft: '10px',
    width: '150px',
  },
  directorySelect: {
    marginLeft: '10px',
    width: '300px',
  },
  processSelect: {
    marginLeft: '10px',
    width: '100px',
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
  date: {
    marginLeft: '10px',
    width: '200px'
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

interface LoggingRouteParams {
}

interface LoggingParams extends RouteComponentProps<LoggingRouteParams> {
  fromUrl?: string
}

const ServiceLogging: React.FC<LoggingParams> = props => {
  const classes = useStyles()

  const [service, setService] = useState<string>()
  const [instance, setInstance] = useState<string>()
  const [directory, setDirectory] = useState<string>()
  const [process, setProcess] = useState<string>()
  const [fromTime, setFromTime] = useState<Date>()
  const [toTime, setToTime] = useState<Date>()
  const [level, setLevel] = useState<string>()
  const [find, setFind] = useState<string>('')
  const [follow, setFollow] = useState<boolean>()

  const [error, setError] = useState<string>()

  const tableRef = useRef<LogsTableEvents>(null)

  useEffect(() => {
    if (service) {
      getInstances({
        variables: { service: service! },
      })
    }
    setInstance(undefined)
    setFromTime(undefined)
    setToTime(undefined)
  }, [ service ])

  useEffect(() => {
    if (service && instance) {
      getDirectories({
        variables: { service: service!, instance: instance! },
      })
    }
    setDirectory(undefined)
  }, [ instance ])

  useEffect(() => {
    if (service && instance && directory) {
      getProcesses({
        variables: { service: service!, instance: instance!, directory: directory! },
      })
    }
    setProcess(undefined)
  }, [ directory ])

  const { data: services } = useLogServicesQuery({
    onError(err) { setError('Query log services error ' + err.message) },
  })

  const [ getInstances, instances ] = useLogInstancesLazyQuery({
    onError(err) { setError('Query log instances error ' + err.message) },
  })

  const [ getDirectories, directories ] = useLogDirectoriesLazyQuery({
    onError(err) { setError('Query log directories error ' + err.message) },
  })

  const [ getProcesses, processes ] = useLogProcessesLazyQuery({
    onError(err) { setError('Query log processes error ' + err.message) },
  })

  const { data: levels } = useLogLevelsQuery({
    variables: { service: service, instance: instance, directory: directory, process: process },
    onError(err) { setError('Query log levels error ' + err.message) },
  })

  const { data: startTime } = useLogsStartTimeQuery({
    variables: { service: service, instance: instance, directory: directory, process: process },
    onCompleted(data) { if (data.logsStartTime) setFromTime(data.logsStartTime) },
    onError(err) { setError('Query log min time error ' + err.message) },
  })

  const { data: endTime } = useLogsEndTimeQuery({
    variables: { service: service, instance: instance, directory: directory, process: process },
    onCompleted(data) { if (data.logsEndTime) setToTime(data.logsEndTime) },
    onError(err) { setError('Query log max time error ' + err.message) },
  })

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
                      disabled={!services?.logServices}
                      control={
                        <Select
                          className={classes.serviceSelect}
                          native
                          onChange={(event) => {
                            setService(event.target.value as string)
                          }}
                          title='Select service'
                          value={service}
                        >
                          <option key={-1}/>
                          { services?.logServices
                              .map((service, index) => <option key={index}>{service}</option>)}
                        </Select>
                      }
                      label='Service'
                    />
                    <FormControlLabel
                      className={classes.control}
                      labelPlacement={'start'}
                      disabled={!service || instances.loading || !instances.data}
                      control={
                        <Select
                          className={classes.instanceSelect}
                          native
                          onChange={(event) => {
                            setInstance(event.target.value as string)
                          }}
                          title='Select instance'
                          value={instance}
                        >
                          <option key={-1}/>
                          { service && !instances.loading && instances.data ? instances.data.logInstances
                            .map((instance, index) => <option key={index}>{instance}</option>) : null }
                        </Select>
                      }
                      label='Instance'
                    />
                    <FormControlLabel
                      className={classes.control}
                      labelPlacement={'start'}
                      disabled={!instance || directories.loading || !directories.data}
                      control={
                        <Select
                          className={classes.directorySelect}
                          native
                          onChange={(event) => {
                            setDirectory(event.target.value as string)
                          }}
                          title='Select directory'
                          value={directory}
                        >
                          <option key={-1}/>
                          { instance && !directories.loading && directories.data ? directories.data.logDirectories
                            .map((directory, index) => <option key={index}>{directory}</option>) : null }
                        </Select>
                      }
                      label='Directory'
                    />
                    <FormControlLabel
                      className={classes.control}
                      labelPlacement={'start'}
                      disabled={!directory || processes.loading || !processes.data }
                      control={
                        <Select
                          className={classes.processSelect}
                          native
                          onChange={(event) => {
                            setProcess(event.target.value as string)
                          }}
                          title='Select process'
                          value={process}
                        >
                          <option key={-1}/>
                          { directory && !processes.loading && processes.data ? processes.data.logProcesses
                            .map((process, index) => <option key={index}>{process}</option>) : null }
                        </Select>
                      }
                      label='Process'
                    />
                    <FormControlLabel
                      className={classes.control}
                      labelPlacement={'start'}
                      disabled={!service}
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
                  </FormGroup>
                  <FormGroup row>
                    <FormControlLabel
                      className={classes.control}
                      labelPlacement={'start'}
                      disabled={!service}
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
                            .map((level, index) => <option key={index}>{level}</option>) : null }
                        </Select>
                      }
                      label='Level'
                    />
                    {!follow ? <FormControlLabel
                      className={classes.control}
                      labelPlacement={'start'}
                      disabled={!service || instances.loading || !instances.data}
                      control={
                        <DateTimePicker
                          className={classes.date}
                          value={fromTime}
                          // minDate={startTime}
                          // maxDate={endTime}
                          ampm={false}
                          onChange={(newValue) => {
                            setFromTime(newValue?newValue:undefined)
                          }}
                        />
                      }
                      label='From'
                    /> : null}
                    {!follow ? <FormControlLabel
                      className={classes.control}
                      labelPlacement={'start'}
                      disabled={!service || instances.loading || !instances.data}
                      control={
                        <DateTimePicker
                          className={classes.date}
                          value={toTime}
                          // minDate={startTime}
                          // maxDate={endTime}
                          ampm={false}
                          onChange={(newValue) => {
                            setToTime(newValue?newValue:undefined)
                          }}
                        />
                      }
                      label='To'
                    /> : null}
                    {!follow ? <FormControlLabel
                      className={classes.control}
                      labelPlacement={'start'}
                      disabled={!service}
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
                      disabled={!service}
                      onClick={() => tableRef.current?.toTop()}
                    >
                      Top
                    </Button> : null}
                    {!follow ? <Button
                      className={classes.bottom}
                      color="primary"
                      variant="contained"
                      disabled={!service}
                      onClick={() => tableRef.current?.toBottom()}
                    >
                      Bottom
                    </Button> : null}
                  </FormGroup>
                </>
              }
              title={'Logs Of Service'}
            />
            <CardContent className={classes.content}>
              <div className={classes.inner}>
                { service && levels && (startTime?.logsStartTime !== undefined) && (endTime?.logsEndTime !== undefined) ?
                  <LogsTable ref={tableRef}
                             className={classes.logsTable}
                             service={service} instance={instance} directory={directory} process={process}
                             fromTime={fromTime} toTime={toTime}
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

export default ServiceLogging