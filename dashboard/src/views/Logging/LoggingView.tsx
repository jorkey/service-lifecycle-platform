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
  AccountRole,
  useLogDirectoriesLazyQuery,
  useLogInstancesLazyQuery, useLogProcessesLazyQuery,
  useLogServicesQuery, useLogsLazyQuery
} from "../../generated/graphql";
import {LogsTable} from "../../common/components/logsTable/LogsTable";

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
    width: '150px',
  },
  instanceSelect: {
    width: '150px',
  },
  directorySelect: {
    width: '300px',
  },
  processSelect: {
    width: '100px',
  },
  findText: {
    width: '200px',
  },
  logsTable: {
    height: 'calc(100vh - 250px)',
  },
  control: {
    paddingLeft: '5px',
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
  const [findText, setFindText] = useState<string>('')
  const [follow, setFollow] = useState<boolean>()

  const [from, setFrom] = useState<number>()
  const [to, setTo] = useState<number>()

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

  return (
    <div className={classes.root}>
      <Grid container spacing={3}>
        <Grid item xs={12}>
          <Card>
            <CardHeader
              action={
                <FormGroup row>
                  <FormControlLabel
                    className={classes.control}
                    labelPlacement={'top'}
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
                    labelPlacement={'top'}
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
                    labelPlacement={'top'}
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
                    labelPlacement={'top'}
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
                    labelPlacement={'top'}
                    disabled={!service}
                    control={
                      <TextField
                        className={classes.findText}
                        onChange={(event) => {
                          setFindText(event.target.value)
                        }}
                        title='Find Text'
                        value={findText}
                      />
                    }
                    label='Find Text'
                  />
                  <FormControlLabel
                    className={classes.control}
                    labelPlacement={'top'}
                    control={
                      <Checkbox
                        className={classes.findText}
                        onChange={ event => setFollow(event.target.checked) }
                        title='Follow'
                        value={follow}
                      />
                    }
                    label='Follow'
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
                           findText={findText != ''?findText:undefined}
                           follow={follow}
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