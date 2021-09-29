import React, {useEffect, useState} from 'react';
import { makeStyles } from '@material-ui/styles';
import {
  Card,
  CardContent, CardHeader, Checkbox, Grid, Select, TextField,
} from '@material-ui/core';
import Alert from "@material-ui/lab/Alert";
import FormGroup from "@material-ui/core/FormGroup";
import FormControlLabel from "@material-ui/core/FormControlLabel";
import {RouteComponentProps} from "react-router-dom";
import {
  useLogDirectoriesLazyQuery,
  useLogInstancesLazyQuery, useLogLevelsQuery, useLogMaxTimeQuery, useLogMinTimeQuery, useLogProcessesLazyQuery,
  useLogServicesQuery
} from "../../generated/graphql";
import {DateTimePicker} from "@material-ui/pickers";
import {LogsTable} from "../../common/components/logsTable/LogsTable";
// import {LogsTable} from "../../common/components/logsTable/LogsTable";
// import {DateTimePicker, LocalizationProvider} from "@mui/lab";
// import {TextField} from "@mui/material";
// import AdapterDateFns from '@mui/lab/AdapterDateFns'

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
    width: '270px',
  },
  follow: {
    marginLeft: '10px',
    width: '50px',
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

interface LoggingViewParams extends RouteComponentProps<LoggingRouteParams> {
  fromUrl: string
}

const LoggingView: React.FC<LoggingViewParams> = props => {
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

  useEffect(() => {
    if (service) {
      getInstances({
        variables: { service: service! },
      })
    }
    setInstance(undefined)
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
    fetchPolicy: 'no-cache',
    onError(err) { setError('Query log services error ' + err.message) },
  })

  const [ getInstances, instances ] = useLogInstancesLazyQuery({
    fetchPolicy: 'no-cache',
    onError(err) { setError('Query log instances error ' + err.message) },
  })

  const [ getDirectories, directories ] = useLogDirectoriesLazyQuery({
    fetchPolicy: 'no-cache',
    onError(err) { setError('Query log directories error ' + err.message) },
  })

  const [ getProcesses, processes ] = useLogProcessesLazyQuery({
    fetchPolicy: 'no-cache',
    onError(err) { setError('Query log processes error ' + err.message) },
  })

  const { data: levels } = useLogLevelsQuery({
    variables: { service: service, instance: instance, directory: directory, process: process },
    fetchPolicy: 'no-cache',
    onError(err) { setError('Query log levels error ' + err.message) },
  })

  const { data: minTime } = useLogMinTimeQuery({
    variables: { service: service, instance: instance, directory: directory, process: process },
    fetchPolicy: 'no-cache',
    onCompleted(data) { if (data.logMinTime) setFromTime(data.logMinTime) },
    onError(err) { setError('Query log min time error ' + err.message) },
  })

  const { data: maxTime } = useLogMaxTimeQuery({
    variables: { service: service, instance: instance, directory: directory, process: process },
    fetchPolicy: 'no-cache',
    onCompleted(data) { if (data.logMaxTime) setToTime(data.logMaxTime) },
    onError(err) { setError('Query log max time error ' + err.message) },
  })

  const sortLevels = () => {
    return levels ? levels.logLevels.sort((l1, l2) => {
      const level1 = l1.toUpperCase()
      const level2 = l2.toUpperCase()
      if (level1 == level2) return 0
      if (level1 == "TRACE") return -1
      if (level2 == "TRACE") return 1
      if (level1 == "DEBUG") return -1
      if (level2 == "DEBUG") return 1
      if (level1 == "INFO") return -1
      if (level2 == "INFO") return 1
      if (level1 == "WARN") return -1
      if (level2 == "WARN") return 1
      if (level1 == "WARNING") return -1
      if (level2 == "WARNING") return 1
      if (level1 == "ERROR") return -1
      if (level2 == "ERROR") return 1
      return 0
    }) : []
  }

  const levelWithSubLevels = () => {
    if (levels && level) {
      const sortedLevels = sortLevels()
      const index = sortedLevels.indexOf(level)
      return index != undefined ? sortedLevels.slice(index) : undefined
    } else {
      return undefined
    }
  }

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
                          { sortLevels()
                            .map((level, index) => <option key={index}>{level}</option>) }
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
                          minDate={minTime}
                          maxDate={maxTime}
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
                          minDate={minTime}
                          maxDate={maxTime}
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
                  </FormGroup>
                </>
              }
              title={'Logs of service'}
            />
            <CardContent className={classes.content}>
              <div className={classes.inner}>
                { service ?
                  <LogsTable className={classes.logsTable}
                             service={service} instance={instance} directory={directory} process={process}
                             fromTime={fromTime} toTime={toTime}
                             levels={levelWithSubLevels()}
                             find={find != ''?find:undefined}
                             follow={follow}
                             onComplete={() => {}}
                             onError={message => {}}
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

export default LoggingView