import React, {useEffect, useRef, useState} from 'react';
import { makeStyles } from '@material-ui/styles';
import {
  Button,
  Card,
  CardContent, CardHeader, Checkbox, Select, TextField,
} from '@material-ui/core';
import Alert from "@material-ui/lab/Alert";
import FormGroup from "@material-ui/core/FormGroup";
import FormControlLabel from "@material-ui/core/FormControlLabel";
import {RouteComponentProps} from "react-router-dom";
import {
  useLogDirectoriesLazyQuery,
  useLogInstancesLazyQuery, useLogProcessesLazyQuery,
  useLogServicesQuery, useLogLevelsLazyQuery
} from "../../../../generated/graphql";
import {DateTimePicker} from "@material-ui/pickers";
import {LogsTable, LogsTableEvents} from "../../../../common/components/logsTable/LogsTable";
import {Logs} from "../../../../common/utils/Logs";
import {download} from "../../../../common/load/Download";
import DownloadIcon from "@material-ui/icons/CloudDownload";

const useStyles = makeStyles((theme:any) => ({
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
  },
  bottom: {
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

interface LoggingParams extends RouteComponentProps<LoggingRouteParams> {
  fromUrl?: string
}

const ServiceLogging: React.FC<LoggingParams> = props => {
  const classes = useStyles()

  const [service, setService] = useState<string>()
  const [instance, setInstance] = useState<string>()
  const [directory, setDirectory] = useState<string>()
  const [pid, setPid] = useState<string>()
  const [fromTime, setFromTime] = useState<Date>()
  const [toTime, setToTime] = useState<Date>()
  const [level, setLevel] = useState<string>()
  const [find, setFind] = useState<string>('')
  const [inputFind, setInputFind] = useState<string>('')
  const [follow, setFollow] = useState<boolean>()

  const [error, setError] = useState<string>()

  const tableRef = useRef<LogsTableEvents>(null)

  const development = process.env.NODE_ENV === 'development';

  useEffect(() => {
    if (service) {
      if (instance && directory) {
        getProcesses({
          variables: { service: service!, instance: instance!, directory: directory! },
        })
      } else if (instance) {
        getDirectories({
          variables: { service: service!, instance: instance! },
        })
      } else {
        getInstances({
          variables: { service: service! },
        })
      }
      getLevels({
        variables: { service: service!, instance: instance, directory: directory, process: pid },
      })
    }
    setLevel(undefined)
  }, [ service, instance, directory, process ])

  const { data: services } = useLogServicesQuery({
    fetchPolicy: 'no-cache', // base option no-cache does not work
    onError(err) { setError('Query log services error ' + err.message) },
  })

  const [ getInstances, instances ] = useLogInstancesLazyQuery({
    fetchPolicy: 'no-cache', // base option no-cache does not work
    onError(err) { setError('Query log instances error ' + err.message) },
  })

  const [ getDirectories, directories ] = useLogDirectoriesLazyQuery({
    fetchPolicy: 'no-cache', // base option no-cache does not work
    onError(err) { setError('Query log directories error ' + err.message) },
  })

  const [ getProcesses, processes ] = useLogProcessesLazyQuery({
    fetchPolicy: 'no-cache', // base option no-cache does not work
    onError(err) { setError('Query log processes error ' + err.message) },
  })

  const [ getLevels, levels ] = useLogLevelsLazyQuery({
    fetchPolicy: 'no-cache', // base option no-cache does not work
    onError(err) { setError('Query log levels error ' + err.message) }
  })

  useEffect(() => {
    const timeOutId = setTimeout(() => setFind(inputFind), 1000)
    return () => clearTimeout(timeOutId)
  }, [inputFind])

  return (
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
                      setService(event.target.value?event.target.value as string:undefined)
                      setInstance(undefined)
                      setDirectory(undefined)
                      setPid(undefined)
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
                      setInstance(event.target.value?event.target.value as string:undefined)
                      setDirectory(undefined)
                      setPid(undefined)
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
                      setDirectory(event.target.value?event.target.value as string:undefined)
                      setPid(undefined)
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
                      setPid(event.target.value?event.target.value as string:undefined)
                    }}
                    title='Select process'
                    value={pid}
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
                disabled={!service || !levels}
                control={
                  <Select
                    className={classes.level}
                    native
                    onChange={(event) => {
                      setLevel(event.target.value?event.target.value as string:undefined)
                    }}
                    title='Select level'
                    value={level}
                  >
                    <option key={-1}/>
                    { levels.data?.logLevels != undefined ? Logs.sortLevels(levels.data.logLevels)
                      .map((level, index) => <option key={index}>{level}</option>) : null }
                  </Select>
                }
                label='Level'
              />
              {!follow ? <FormControlLabel
                className={classes.control}
                labelPlacement={'start'}
                disabled={!service}
                control={
                  <DateTimePicker
                    className={classes.date}
                    value={fromTime?fromTime:null}
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
                disabled={!service}
                control={
                  <DateTimePicker
                    className={classes.date}
                    value={toTime?toTime:null}
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
              <FormControlLabel
                className={classes.control}
                labelPlacement={'start'}
                disabled={!service}
                control={
                  <TextField
                    className={classes.find}
                    onChange={(event) => {
                      setInputFind(event.target.value)
                    }}
                    title='Find Text'
                    value={inputFind}
                  />
                }
                label='Find Text'
              />
              <Button disabled={!service}
                      onClick={
                        () => {
                          let params = `service=${encodeURIComponent(service!)}`
                          if (instance) params += `&instance=${encodeURIComponent(instance!)}`
                          if (directory) params += `&directory=${encodeURIComponent(directory)}`
                          if (pid) params += `&process=${encodeURIComponent(pid)}`
                          if (fromTime) params += `&fromTime=${encodeURIComponent(fromTime.toISOString())}`
                          if (toTime) params += `&toTime=${encodeURIComponent(toTime.toISOString())}`
                          if (find) params += `&find=${encodeURIComponent(find)}`
                          if (level && levels.data) {
                            let levelsStr = ''
                            Logs.levelWithSubLevels(level, levels.data.logLevels)?.forEach(
                              level => levelsStr += ':' + level)
                            params += `&levels=${encodeURIComponent(levelsStr)}`
                          }
                          download(`http://${development?'localhost:8000':window.location.host}/load/logs?${params}`,
                            `${service}.log`)
                        }
                      }>
                <DownloadIcon/>
              </Button>
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
          { service ?
            <LogsTable ref={tableRef}
                       className={classes.logsTable}
                       service={service} instance={instance} directory={directory} process={pid}
                       fromTime={fromTime} toTime={toTime}
                       levels={(levels.data && level)?Logs.levelWithSubLevels(level, levels.data.logLevels):undefined}
                       find={find != ''?find:undefined}
                       follow={follow}
                       onComplete={() => {}}
                       onError={error => {setError(error)}}
            /> : null }
          {error && <Alert className={classes.alert} severity="error">{error}</Alert>}
        </div>
      </CardContent>
    </Card>
  )
}

export default ServiceLogging