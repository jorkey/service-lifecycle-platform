import React from "react";
import {
  LogLine,
  Scalars,
  SequencedServiceLogLine, useSubscribeLogsSubscription
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
  from: BigInt
  onLine: (sequence: BigInt, line: LogLine) => void
  onComplete: () => void
}

export const LogsSubscriber = (props: LogsSubscriptionParams) => {
  const { service, instance, process, directory, task, from, onLine, onComplete } = props

  useSubscribeLogsSubscription({
    variables: { service, instance, process, directory, task, from },
    fetchPolicy: 'no-cache',
    onSubscriptionData(data) {
      if (data.subscriptionData.data) {
        onLine(data.subscriptionData.data.subscribeLogs.sequence, data.subscriptionData.data.subscribeLogs.payload)
      }
    },
    onSubscriptionComplete() {
      onComplete()
    }
  })

  return null
}
