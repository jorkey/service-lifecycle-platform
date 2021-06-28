import React, {useState} from "react";
import {
  LogLine,
  SequencedLogLine,
  useSubscribeTaskLogsSubscription, useTaskLogsQuery
} from "../../../generated/graphql";
import {LogsTable} from "./LogsTable";
import Alert from "@material-ui/lab/Alert";
import {makeStyles} from "@material-ui/core/styles";
import {dark} from "@material-ui/core/styles/createPalette";

const useStyles = makeStyles(theme => ({
  alert: {
    marginTop: 25
  }
}));

interface TaskLogsParams {
  task: string
  terminated: () => void
}

export const TaskLogs = (props: TaskLogsParams) => {
  const { task, terminated } = props
  const [error, setError] = useState<string>()
  const classes = useStyles()

  const { data: taskLogs } = useTaskLogsQuery({
    variables: { task: task },
    fetchPolicy: 'no-cache',
    onError(err) { setError('Query service logs error ' + err.message) },
    onCompleted(data) {
      if (data.taskLogs) {
        setError(undefined)
        if (data.taskLogs.find(log => log.line.terminationStatus)) {
          terminated()
        }
      }
    }
  })

  return (<>
    { (taskLogs && taskLogs?.taskLogs) ? <TaskLogsSubscription
        taskId={task}
        lines={taskLogs.taskLogs}
        terminated={() => terminated()}/> : null }
    { error && <Alert className={classes.alert} severity='error'>{error}</Alert>}
  </>)
}

interface TaskLogsSubscriptionParams {
  taskId: string
  lines: SequencedLogLine[]
  terminated: () => void
}

export const TaskLogsSubscription = (props: TaskLogsSubscriptionParams) => {
  const { taskId, lines, terminated } = props
  const [logLines, setLogLines] = useState<LogLine[]>(lines.map(line => line.line))
  const from = (lines.length == 0) ? 0 : lines[lines.length-1].sequence

  useSubscribeTaskLogsSubscription({
    variables: { task: taskId, from: from },
    fetchPolicy: 'no-cache',
    onSubscriptionData(data) {
      if (data.subscriptionData.data) {
        const line = data.subscriptionData.data.subscribeTaskLogs.line
        if (line.terminationStatus) {
          terminated()
        }
        setLogLines([...logLines, line])
      }
    },
    onSubscriptionComplete() {}
  })

  return <LogsTable lines={logLines}/>
}
