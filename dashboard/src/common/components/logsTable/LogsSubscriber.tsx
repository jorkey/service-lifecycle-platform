import React from "react";
import {
  SequencedLogLine, useSubscribeLogsSubscription
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

interface LogsSubscriptionParams extends FindLogsDashboardParams {
  from: number
  onLine: (line: SequencedLogLine) => void
  onComplete: () => void
}

export const LogsSubscriber = (props: LogsSubscriptionParams) => {
  const { service, instance, process, directory, task, from, onLine, onComplete } = props

  console.log(`LogsSubscriber task ${task} from ${from}`)

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
