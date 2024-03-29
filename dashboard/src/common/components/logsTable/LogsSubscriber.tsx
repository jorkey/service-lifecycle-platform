import React from "react";
import {
  useSubscribeLogsSubscription
} from "../../../generated/graphql";
import {FindLogsDashboardParams, LogRecord} from "./LogsTable";

interface LogsSubscriptionParams extends FindLogsDashboardParams {
  onLines: (lines: LogRecord[]) => void
  onComplete: () => void
}

export const LogsSubscriber = (props: LogsSubscriptionParams) => {
  const { service, instance, process, directory, task, levels, unit, find, onLines, onComplete } = props

  useSubscribeLogsSubscription({
    variables: { service, instance, process, directory, task, unit, find, levels },
    onSubscriptionData(data) {
      if (data.subscriptionData.data) {
        onLines(data.subscriptionData.data.subscribeLogs as LogRecord[])
      }
    },
    onSubscriptionComplete() {
      onComplete()
    }
  })

  return null
}
