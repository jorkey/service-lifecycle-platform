import React, {useEffect, useState} from 'react';
import clsx from 'clsx';
import { makeStyles } from '@material-ui/styles';
import {
  Card,
  CardContent, CardHeader, Grid, Select,
} from '@material-ui/core';
import Alert from "@material-ui/lab/Alert";
import FormGroup from "@material-ui/core/FormGroup";
import FormControlLabel from "@material-ui/core/FormControlLabel";
import {RouteComponentProps, useHistory} from "react-router-dom";
import {
  useLogDirectoriesLazyQuery,
  useLogInstancesLazyQuery, useLogProcessesLazyQuery,
  useLogServicesQuery, useLogsLazyQuery
} from "../../generated/graphql";
import {LogsTable} from "../../common/components/logsTable/LogsTable";

const useStyles = makeStyles((theme:any) => ({
  root: {},
  content: {
    padding: 0
  },
  inner: {
    minWidth: 800
  },
  serviceSelect: {
    width: '150px',
    paddingRight: '2px'
  },
  instanceSelect: {
    width: '150px',
    paddingRight: '2px'
  },
  directorySelect: {
    width: '150px',
    paddingRight: '2px'
  },
  processSelect: {
    width: '150px',
    paddingRight: '2px'
  },
  logsTable: {
    height: 'calc(100vh - 180px)',
  },
  control: {
    paddingLeft: '10px',
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

  const [from, setFrom] = useState<number>()
  const [to, setTo] = useState<number>()

  const [error, setError] = useState<string>()

  useEffect(() => {
    getInstances()
    setInstance(undefined)
  }, [ service ])

  useEffect(() => {
    getDirectories()
    setDirectory(undefined)
  }, [ instance ])

  useEffect(() => {
    getProcesses()
    setProcess(undefined)
  }, [ directory ])

  const { data: services } = useLogServicesQuery({
    fetchPolicy: 'no-cache',
    onError(err) { setError('Query log services error ' + err.message) },
    onCompleted() { setError(undefined) }
  })

  const [ getInstances, instances ] = useLogInstancesLazyQuery({
    variables: { service: service! },
    fetchPolicy: 'no-cache',
    onError(err) { setError('Query log instances error ' + err.message) },
    onCompleted() { setError(undefined) }
  })

  const [ getDirectories, directories ] = useLogDirectoriesLazyQuery({
    variables: { service: service!, instance: instance! },
    fetchPolicy: 'no-cache',
    onError(err) { setError('Query log directories error ' + err.message) },
    onCompleted() { setError(undefined) }
  })

  const [ getProcesses, processes ] = useLogProcessesLazyQuery({
    variables: { service: service!, instance: instance!, directory: directory! },
    fetchPolicy: 'no-cache',
    onError(err) { setError('Query log pocesses error ' + err.message) },
    onCompleted() { setError(undefined) }
  })

  useLogsLazyQuery({
    variables: { service: service, instance: instance, directory: directory, process: process,
      fromTime: fromTime, toTime: toTime, from: from, to: to },
    fetchPolicy: 'no-cache',
    onError(err) { setError('Query log directories error ' + err.message) },
    onCompleted() { setError(undefined) }
  })

  return (
    <div className={classes.root}>
      <Grid container spacing={3}>
        <Grid item xs={12}>
          <Card
            className={clsx(classes.root)}
          >
            <CardHeader
              action={
                <FormGroup row>
                  <FormControlLabel
                    className={classes.control}
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
                    disabled={!service || instances.loading || !instances.data}
                    control={
                      <Select
                        className={classes.instanceSelect}
                        native
                        onChange={(event) => {
                          setService(event.target.value as string)
                        }}
                        title='Select instance'
                        value={service}
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
                    disabled={!instance || directories.loading || !directories.data}
                    control={
                      <Select
                        className={classes.directorySelect}
                        native
                        onChange={(event) => {
                          setService(event.target.value as string)
                        }}
                        title='Select directory'
                        value={service}
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
                    disabled={!directory || processes.loading || !processes.data }
                    control={
                      <Select
                        className={classes.processSelect}
                        native
                        onChange={(event) => {
                          setService(event.target.value as string)
                        }}
                        title='Select process'
                        value={service}
                      >
                        <option key={-1}/>
                        { directory && !processes.loading && processes.data ? processes.data.logProcesses
                          .map((process, index) => <option key={index}>{process}</option>) : null }
                      </Select>
                    }
                    label='Process'
                  />
                </FormGroup>
              }
              title={'Logs of services'}
            />
            <CardContent className={classes.content}>
              <div className={classes.inner}>
                <LogsTable className={classes.logsTable}
                           service={service} instance={instance} directory={directory} process={process}
                           fromTime={fromTime} toTime={toTime}
                           onComplete={() => {}}
                           onError={message => {}}
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

export default LoggingView