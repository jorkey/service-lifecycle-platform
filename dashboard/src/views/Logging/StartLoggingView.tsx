import React, {useState} from 'react';
import clsx from 'clsx';
import { makeStyles } from '@material-ui/styles';
import {
  Card,
  CardContent, CardHeader, Select,
} from '@material-ui/core';
import Alert from "@material-ui/lab/Alert";
import FormGroup from "@material-ui/core/FormGroup";
import FormControlLabel from "@material-ui/core/FormControlLabel";
import {RouteComponentProps, useHistory} from "react-router-dom";
import {
  useLogDirectoriesLazyQuery,
  useLogInstancesLazyQuery,
  useLogServicesLazyQuery,
  useLogsLazyQuery
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
    marginTop: '20px'
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

interface StartLoggingViewParams extends RouteComponentProps<LoggingRouteParams> {
  fromUrl: string
}

const StartLoggingView: React.FC<StartLoggingViewParams> = props => {
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

  const [ getServices, services ] = useLogServicesLazyQuery({
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

  useLogsLazyQuery({
    variables: { service: service, instance: instance, directory: directory, process: process,
      fromTime: fromTime, toTime: toTime, from: from, to: to },
    fetchPolicy: 'no-cache',
    onError(err) { setError('Query log directories error ' + err.message) },
    onCompleted() { setError(undefined) }
  })

  const history = useHistory()

  return (
    <Card
      className={clsx(classes.root)}
    >
      <CardHeader
        action={
          <FormGroup row>
            { services?.providersInfo.length ?
              <FormControlLabel
                className={classes.control}
                control={
                  <Select
                    className={classes.providerSelect}
                    native
                    onChange={(event) => {
                      const distribution = providers?.providersInfo.find(provider => provider.distribution == event.target.value as string)
                      setProvider(distribution)
                    }}
                    title='Select provider'
                    value={provider?.distribution}
                  >
                    <option key={-1}/>
                    { providers?.providersInfo
                        .map((provider) => provider.distribution)
                        .map((provider, index) => <option key={index}>{provider}</option>)}
                  </Select>
                }
                label='Update From Provider'
              /> : null }
            <RefreshControl
              className={classes.control}
              refresh={ () => {
                if (provider) {
                  getProviderVersions({ variables: { distribution: provider.distribution } })
                }
                getDeveloperVersions()
                getClientVersions() }}
            />
          </FormGroup>
        }
        title={provider?'Update Client Services':'Build Client Services'}
      />
      <CardContent className={classes.content}>
        <div className={classes.inner}>
          <LogsTable service={service} instance={instance} directory={directory} process={process}
                     fromTime={fromTime} toTime={toTime}
                     onComplete={}
          />
          {error && <Alert className={classes.alert} severity="error">{error}</Alert>}
        </div>
      </CardContent>
    </Card>
  )
}

export default StartLoggingView