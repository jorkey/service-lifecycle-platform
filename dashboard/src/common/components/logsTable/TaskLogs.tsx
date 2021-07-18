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
  task: string,
  onTerminated: (date: Date, status: boolean) => void
}

export const TaskLogs = (props: TaskLogsParams) => {
  const { task, onTerminated } = props
  const [error, setError] = useState<string>()
  const [terminated, setTerminated] = useState(false)
  const classes = useStyles()

  const { data: taskLogs } = useTaskLogsQuery({
    variables: { task: task },
    fetchPolicy: 'no-cache',
    onError(err) { setError('Query service logs error ' + err.message) },
    onCompleted(data) {
      if (data.taskLogs) {
        setError(undefined)
        const last = data.taskLogs.find(log => log.line.terminationStatus)
        if (last) {
          setTerminated(true)
          onTerminated(last.line.time, last.line.terminationStatus!)
        }
      }
    }
  })

  return (<>
    { (taskLogs && taskLogs?.taskLogs && !terminated) ? <TaskLogsSubscription
        taskId={task}
        lines={taskLogs.taskLogs}
        onTerminated={(date, status) => onTerminated(date, status)}/> : null }
    { error && <Alert className={classes.alert} severity='error'>{error}</Alert>}
  </>)
}

interface TaskLogsSubscriptionParams {
  taskId: string,
  lines: SequencedLogLine[],
  onTerminated: (date: Date, status: boolean) => void
}

export const TaskLogsSubscription = (props: TaskLogsSubscriptionParams) => {
  const { taskId, lines, onTerminated } = props
  const [logLines, setLogLines] = useState<LogLine[]>(lines.map(line => line.line))
  const [terminated, setTerminated] = useState(false)
  const from = (lines.length == 0) ? 0 : (lines[lines.length-1].sequence + 1)

  useSubscribeTaskLogsSubscription({
    variables: { task: taskId, from: from },
    fetchPolicy: 'no-cache',
    onSubscriptionData(data) {
      if (!terminated && !data.subscriptionData.loading && data.subscriptionData.data) {
        const line = data.subscriptionData.data.subscribeTaskLogs.line
        if (line.terminationStatus != undefined) {
          setTerminated(true)
          onTerminated(line.time, line.terminationStatus)
        }
        setLogLines([...logLines, line])
      }
    },
    onSubscriptionComplete() {
      if (!terminated) {
        setTerminated(true)
        onTerminated(new Date(), false)
      }
    }
  })

  return <LogsTable lines={logLines}/>
}
