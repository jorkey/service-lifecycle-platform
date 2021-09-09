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
  subscribe?: boolean
}

interface FindLogsParams extends FindLogsDashboardParams {
  from?: number
  to?: number
}

interface LogsGetterParams extends FindLogsParams {
  onLines: (lines: SequencedLogLine[]) => void
  onComplete: () => void
  onError: (message: string) => void
}

export const LogsGetter = (props: LogsGetterParams) => {
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
      <LogsSubscription
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

const LogsSubscription = (props: LogsSubscriptionParams) => {
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
