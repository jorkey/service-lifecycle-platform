import React, {useState} from "react";
import {
  LogLine,
  SequencedLogLine,
  useSubscribeTaskLogsSubscription, useTaskLogsQuery
} from "../../../generated/graphql";
import {LogsTable} from "./LogsTable";
import Alert from "@material-ui/lab/Alert";
import {makeStyles} from "@material-ui/core/styles";

const useStyles = makeStyles(theme => ({
  alert: {
    marginTop: 25
  }
}));

interface TaskLogsParams {
  taskId: string
}

export const TaskLogs = (props: TaskLogsParams) => {
  const { taskId } = props
  const [error, setError] = useState<string>()
  const classes = useStyles()

  const { data: taskLogs } = useTaskLogsQuery({
    variables: { task: taskId },
    fetchPolicy: 'no-cache',
    onError(err) { setError('Query service logs error ' + err.message) },
    onCompleted() { setError(undefined) }
  })

  return (<>
    { (taskLogs && taskLogs?.taskLogs) ? <TaskLogsSubscription taskId={taskId} lines={taskLogs.taskLogs}/> : null }
    { error && <Alert className={classes.alert} severity='error'>{error}</Alert>}
  </>)
}

interface TaskLogsSubscriptionParams {
  taskId: string
  lines: SequencedLogLine[]
}

export const TaskLogsSubscription = (props: TaskLogsSubscriptionParams) => {
  const { taskId, lines } = props
  const [logLines, setLogLines] = useState<LogLine[]>(lines.map(line => line.line))
  const from = (lines.length == 0) ? 0 : lines[lines.length-1].sequence

  useSubscribeTaskLogsSubscription({
    variables: { task: taskId, from: from },
    fetchPolicy: 'no-cache',
    onSubscriptionData(data) {
      if (data.subscriptionData.data) {
        setLogLines([...logLines, data.subscriptionData.data.subscribeTaskLogs.line])
      }
    },
    onSubscriptionComplete() {}
  })

  return <LogsTable lines={logLines}/>
}
