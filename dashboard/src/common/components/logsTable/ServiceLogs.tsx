import React, {useState} from "react";
import {
  LogLine, Scalars, SequencedLogLine, useServiceLogsQuery,
  useSubscribeServiceLogsSubscription,
  useSubscribeTaskLogsSubscription
} from "../../../generated/graphql";
import {LogsTable} from "./LogsTable";
import {makeStyles} from "@material-ui/core/styles";
import Alert from "@material-ui/lab/Alert";
import {TaskLogsSubscription} from "./TaskLogs";

const useStyles = makeStyles(theme => ({
  alert: {
    marginTop: 25
  }
}));

interface ServiceLogsParams {
  distribution: string
  service: string
  instance: string
  process: string
  directory: string
}

export const ServiceLogs = (props: ServiceLogsParams) => {
  const { distribution, service, instance, process, directory } = props
  const [error, setError] = useState<string>()
  const classes = useStyles()

  const { data: serviceLogs } = useServiceLogsQuery({
    variables: { distribution: distribution, service: service, instance: instance, process: process, directory: directory },
    fetchPolicy: 'no-cache',
    onError(err) { setError('Query service logs error ' + err.message) },
    onCompleted() { setError(undefined) }
  })

  if (serviceLogs?.serviceLogs) {
    return <ServiceLogsSubscription distribution={distribution} service={service} instance={instance} process={process}
                                    directory={directory} lines={serviceLogs.serviceLogs}/>
  }
  return (<>
    { (serviceLogs && serviceLogs?.serviceLogs) ? <ServiceLogsSubscription distribution={distribution} service={service} instance={instance} process={process}
                                                                           directory={directory} lines={serviceLogs.serviceLogs}/> : null }
    { error && <Alert className={classes.alert} severity='error'>{error}</Alert>}
  </>)
}

interface ServiceLogsSubscriptionParams {
  distribution: string
  service: string
  instance: string
  process: string
  directory: string
  lines: SequencedLogLine[]
}

const ServiceLogsSubscription = (props: ServiceLogsSubscriptionParams) => {
  const { distribution, service, instance, process, directory, lines } = props
  const [logLines, setLogLines] = useState<LogLine[]>(lines.map(line => line.line))
  const from = (lines.length == 0) ? 0 : lines[lines.length-1].sequence

  useSubscribeServiceLogsSubscription({
    variables: { distribution, service, instance, process, directory, from },
    fetchPolicy: 'no-cache',
    onSubscriptionData(data) {
      if (data.subscriptionData.data) {
        setLogLines([...logLines, data.subscriptionData.data.subscribeServiceLogs.line])
      }
    },
    onSubscriptionComplete() {}
  })

  return <LogsTable lines={logLines}/>
}
