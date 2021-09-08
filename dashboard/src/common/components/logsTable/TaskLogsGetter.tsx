import React from "react";
import {
  SequencedLogLine, useServiceLogsQuery,
  useSubscribeServiceLogsSubscription, useSubscribeTaskLogsSubscription, useTaskLogsQuery,
} from "../../../generated/graphql";

interface TaskLogsGetterParams extends TaskLogsParams {
  subscribe?: boolean,
  onLines: (lines: SequencedLogLine[]) => void
  onComplete: () => void
  onError: (message: string) => void
}

export const TaskLogsGetter = (props: TaskLogsGetterParams) => {
  const { task,
    subscribe, onLines, onComplete, onError } = props

  const { data: taskLogs } = useTaskLogsQuery({
    variables: {
      task: task
    },
    fetchPolicy: 'no-cache',
    onError(err) {
      onError(err.message)
    },
    onCompleted() {
      if (taskLogs) {
        onLines(taskLogs.taskLogs)
      }
    }
  })

  return (subscribe ? <>
    { (taskLogs && taskLogs?.taskLogs && taskLogs.taskLogs.length) ?
      <TaskLogsSubscription
        {...props}
        fromSequence={ taskLogs?.taskLogs[taskLogs.taskLogs.length-1].sequence+1 }
        onLine={ line => onLines([line]) }
        onComplete={ () => onComplete() }
      /> : null
    }
  </> : null)
}

interface TaskLogsSubscriptionParams extends TaskLogsParams {
  fromSequence: number
  onLine: (line: SequencedLogLine) => void
  onComplete: () => void
}

const TaskLogsSubscription = (props: TaskLogsSubscriptionParams) => {
  const { task, fromSequence, onLine, onComplete } = props

  useSubscribeTaskLogsSubscription({
    variables: { task, fromSequence },
    fetchPolicy: 'no-cache',
    onSubscriptionData(data) {
      if (data.subscriptionData.data) {
        onLine(data.subscriptionData.data.subscribeTaskLogs)
      }
    },
    onSubscriptionComplete() {
      onComplete()
    }
  })

  return null
}
