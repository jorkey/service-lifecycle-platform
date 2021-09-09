import React from "react";
import {
  SequencedLogLine, useLogsQuery, useSubscribeLogsSubscription
} from "../../../generated/graphql";

export interface FindLogsDashboardParams {
  service?: string
  instance?: string
  process?: string
  directory?: string
  task?: string
  fromTime?: Date
  toTime?: Date
}

interface FindLogsParams extends FindLogsDashboardParams {
  from?: number
  to?: number
}

interface ServiceLogsGetterParams extends FindLogsParams {
  subscribe?: boolean,
  onLines: (lines: SequencedLogLine[]) => void
  onComplete: () => void
  onError: (message: string) => void
}

export const ServiceLogsGetter = (props: ServiceLogsGetterParams) => {
  const { service, instance, process, directory, task, from, to, fromTime, toTime,
    subscribe, onLines, onComplete, onError } = props

  const { data: logs } = useLogsQuery({
    variables: {
      service: service, instance: instance, process: process, directory: directory, task: task,
      from: from, to: to, fromTime: fromTime, toTime: toTime
    },
    fetchPolicy: 'no-cache',
    onError(err) {
      onError(err.message)
    },
    onCompleted() {
      if (logs) {
        onLines(logs.logs)
      }
    }
  })

  return (subscribe ? <>
    { (logs && logs?.logs && logs.logs.length) ?
      <ServiceLogsSubscription
        {...props}
        from={ logs?.logs[logs.logs.length-1].sequence+1 }
        onLine={ line => onLines([line]) }
        onComplete={ () => onComplete() }
      /> : null
    }
  </> : null)
}

interface LogsSubscriptionParams extends FindLogsParams {
  from: number
  onLine: (line: SequencedLogLine) => void
  onComplete: () => void
}

const ServiceLogsSubscription = (props: LogsSubscriptionParams) => {
  const { service, instance, process, directory, task, from, onLine, onComplete } = props

  useSubscribeLogsSubscription({
    variables: { service, instance, process, directory, task, from },
    fetchPolicy: 'no-cache',
    onSubscriptionData(data) {
      if (data.subscriptionData.data) {
        onLine(data.subscriptionData.data.subscribeLogs)
      }
    },
    onSubscriptionComplete() {
      onComplete()
    }
  })

  return null
}
