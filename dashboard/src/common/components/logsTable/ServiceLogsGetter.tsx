import React from "react";
import {
  SequencedLogLine, useServiceLogsQuery,
  useSubscribeServiceLogsSubscription,
} from "../../../generated/graphql";

interface ServiceLogsGetterParams extends ServiceLogsParams {
  fromSequence?: number
  toSequence?: number
  subscribe?: boolean,
  onLines: (lines: SequencedLogLine[]) => void
  onComplete: () => void
  onError: (message: string) => void
}

export const ServiceLogsGetter = (props: ServiceLogsGetterParams) => {
  const { service, instance, process, directory,
    fromSequence, toSequence, subscribe,
    onLines, onComplete, onError } = props

  const { data: serviceLogs } = useServiceLogsQuery({
    variables: {
      service: service, instance: instance, process: process, directory: directory,
      fromSequence: fromSequence, toSequence: toSequence
    },
    fetchPolicy: 'no-cache',
    onError(err) {
      onError(err.message)
    },
    onCompleted() {
      if (serviceLogs) {
        onLines(serviceLogs.serviceLogs)
      }
    }
  })

  return (subscribe ? <>
    { (serviceLogs && serviceLogs?.serviceLogs && serviceLogs.serviceLogs.length) ?
      <ServiceLogsSubscription
        {...props}
        fromSequence={ serviceLogs?.serviceLogs[serviceLogs.serviceLogs.length-1].sequence+1 }
        onLine={ line => onLines([line]) }
        onComplete={ () => onComplete() }
      /> : null
    }
  </> : null)
}

interface ServiceLogsSubscriptionParams extends ServiceLogsParams {
  fromSequence: number
  onLine: (line: SequencedLogLine) => void
  onComplete: () => void
}

const ServiceLogsSubscription = (props: ServiceLogsSubscriptionParams) => {
  const { service, instance, process, directory, fromSequence, onLine, onComplete } = props

  useSubscribeServiceLogsSubscription({
    variables: { service, instance, process, directory, fromSequence },
    fetchPolicy: 'no-cache',
    onSubscriptionData(data) {
      if (data.subscriptionData.data) {
        onLine(data.subscriptionData.data.subscribeServiceLogs)
      }
    },
    onSubscriptionComplete() {
      onComplete()
    }
  })

  return null
}
