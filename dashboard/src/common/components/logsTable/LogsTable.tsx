import React, {ForwardedRef, forwardRef, useEffect, useImperativeHandle, useState} from "react";
import {
  LogLine,
  useDirectoryLogsLazyQuery, useInstanceLogsLazyQuery,
  useProcessLogsLazyQuery, useServiceLogsLazyQuery, useTaskLogsLazyQuery,
} from "../../../generated/graphql";
import GridTable from "../gridTable/GridTable";
import {makeStyles} from "@material-ui/core/styles";
import {GridTableColumnParams} from "../gridTable/GridTableColumn";
import {LogsSubscriber} from "./LogsSubscriber";
import {GridTableCellParams} from "../gridTable/GridTableCell";

const useStyles = makeStyles(theme => ({
  div: {
    display: 'relative'
  },
  timeColumn: {
    minWidth: '180px',
    maxWidth: '180px',
    paddingLeft: 0,
  },
  instanceColumn: {
    minWidth: '200px',
    maxWidth: '200px',
  },
  processColumn: {
    minWidth: '100px',
    maxWidth: '100px',
  },
  levelColumn: {
    minWidth: '100px',
    maxWidth: '100px',
  },
  messageColumn: {
    whiteSpace: 'pre',
  },
  inProgress: {
    cursor: 'progress',
  }
}))

export interface FindLogsDashboardParams {
  className: string
  task?: string
  service?: string
  instance?: string
  process?: string
  directory?: string
  fromTime?: Date
  toTime?: Date
  levels?: string[]
  find?: string
  follow?: boolean
}

interface LogsTableParams extends FindLogsDashboardParams {
  onComplete: (time: Date, status: boolean) => void
  onError: (message: string) => void
}

export interface LogsTableEvents {
  toTop: () => void
  toBottom: () => void
}

export interface LogRecord {
  sequence: BigInt
  instance?: string
  directory?: string
  process?: string
  payload: LogLine
}

export interface FindLogsDashboardParams {
  service?: string
  instance?: string
  process?: string
  directory?: string
  task?: string
  levels?: string[]
  fromTime?: Date
  toTime?: Date
  subscribe?: boolean
}

export const LogsTable = forwardRef((props: LogsTableParams, ref: ForwardedRef<LogsTableEvents>) => {
  const { className, service, instance, process, directory, task, fromTime, toTime, levels, find,
    follow, onComplete, onError } = props

  const [ lines, setLines ] = useState<LogRecord[]>([])

  const [ toBottomState, setToBottomState ] = useState(0)
  const [ terminationStatus, setTerminationStatus ] = useState<boolean>()

  useImperativeHandle(ref, () => ({
    toTop: () => {
      setLines([])
      getLogs(BigInt(0))
    },
    toBottom: () => {
      setLines([])
      setToBottomState(1)
      getLogs(undefined, BigInt('0x7FFFFFFFFFFFFFFF'))
    }
  }))

  const sliceRowsCount = 100

  const [ getTaskLogs, taskLogs ] = useTaskLogsLazyQuery({
    fetchPolicy: 'no-cache', // base option no-cache does not work
    onError(err) { onError(err.message) },
    onCompleted(data) { if (data.logs) { addLines(data.logs) } }
  })

  const [ getServiceLogs, serviceLogs ] = useServiceLogsLazyQuery({
    fetchPolicy: 'no-cache', // base option no-cache does not work
    onError(err) { onError(err.message) },
    onCompleted(data) { if (data.logs) { addLines(data.logs) } }
  })

  const [ getInstanceLogs, instanceLogs ] = useInstanceLogsLazyQuery({
    fetchPolicy: 'no-cache', // base option no-cache does not work
    onError(err) { onError(err.message) },
    onCompleted(data) { if (data.logs) { addLines(data.logs) } }
  })

  const [ getDirectoryLogs, directoryLogs ] = useDirectoryLogsLazyQuery({
    fetchPolicy: 'no-cache', // base option no-cache does not work
    onError(err) { onError(err.message) },
    onCompleted(data) { if (data.logs) { addLines(data.logs) } }
  })

  const [ getProcessLogs, processLogs ] = useProcessLogsLazyQuery({
    fetchPolicy: 'no-cache', // base option no-cache does not work
    onError(err) { onError(err.message) },
    onCompleted(data) { if (data.logs) { addLines(data.logs) } }
  })

  const isLoading = () => serviceLogs.loading || taskLogs.loading || instanceLogs.loading || directoryLogs.loading || processLogs.loading

  const getCommonVariables = (from?: BigInt, to?: BigInt) => {
    return {
      fromTime: fromTime, toTime: toTime, levels: levels, find: find,
      from: from, to: to, limit: sliceRowsCount
    }
  }

  const getLogs = (from?: BigInt, to?: BigInt) => {
    if (task) {
      getTaskLogs({variables: {task: task, ...getCommonVariables(from, to)}})
    } else if (service && instance && directory && process) {
      getProcessLogs({ variables: { service: service, instance: instance, directory: directory, process: process, ...getCommonVariables(from, to) }  })
    } else if (service && instance && directory) {
      getDirectoryLogs({ variables: { service: service, instance: instance, directory: directory, ...getCommonVariables(from, to) }  })
    } else if (service && instance) {
      getInstanceLogs({ variables: { service: service, instance: instance, ...getCommonVariables(from, to) }  })
    } else if (service) {
      getServiceLogs({ variables: { service: service, ...getCommonVariables(from, to) }  })
    }
  }

  useEffect(() => {
    setLines([])
    if (!follow) {
      getLogs(BigInt(0))
    }
  },  [ service, instance, directory, process, task, fromTime, toTime, levels, find, follow ])

  const classes = useStyles()

  const columns: GridTableColumnParams[] = [
    {
      name: 'time',
      headerName: 'Time',
      className: classes.timeColumn,
      type: 'date',
    },
    {
      name: 'level',
      headerName: 'Level',
      className: classes.levelColumn
    },
    {
      name: 'instance',
      headerName: 'Instance',
      className: classes.instanceColumn
    },
    {
      name: 'process',
      headerName: 'Process',
      className: classes.processColumn
    },
    {
      name: 'message',
      headerName: 'Line',
      className: classes.messageColumn
    },
  ].filter(column => column.name !== 'instance' || (!instance && !task))
   .filter(column => column.name !== 'process' || (!process && !task)) as GridTableColumnParams[]

  const addLines = (receivedLines: LogRecord[]) => {
    const begin = lines.length ? lines[0].sequence : BigInt(0)
    const insert = receivedLines.filter(line => line.sequence < begin)
    let newLines = new Array(...lines)
    if (insert.length) {
      newLines = new Array(...insert, ...lines)
    }
    const end = lines.length ? lines[lines.length-1].sequence : BigInt(0)
    const append = receivedLines.filter(line => line.sequence > end)
    if (append.length) {
      newLines = new Array(...newLines, ...append)
    }
    if (follow && newLines.length > 50) {
      newLines.slice(newLines.length - 50)
    }
    setLines(newLines)
    if (newLines.length) {
      const status = newLines[newLines.length-1].payload.terminationStatus
      if (status != undefined) {
        setTerminationStatus(status)
        onComplete(newLines[0].payload.time, status)
      }
    }
    if (toBottomState == 1) {
      setToBottomState(2)
    } else if (toBottomState == 2) {
      setToBottomState(0)
    }
  }

  const rows = lines
    .map(line => (
      new Map<string, GridTableCellParams>([
        ['time', { value: line.payload.time }],
        ['instance', { value: line.instance?line.instance:'' }],
        ['process', { value: line.process?line.process:'' }],
        ['level', { value: line.payload.level }],
        ['unit', { value: line.payload.unit }],
        ['message', { value: line.payload.message }]
      ])))

  return <>
    <GridTable
      className={className + (isLoading() ? ' ' + classes.inProgress : '')}
      columns={columns}
      rows={rows}
      scrollToLastRow={follow || toBottomState == 2}
      onScrollTop={() => {
        if (lines.length) {
          getLogs(undefined, lines[0].sequence)
        }
      }}
      onScrollBottom={() => {
        if (!follow && lines.length && lines[lines.length - 1].payload.terminationStatus == undefined) {
          getLogs(lines[lines.length - 1].sequence, undefined)
        }
      }}
    />
    {follow ?
      <LogsSubscriber
        {...props}
        onLines={(lines) => addLines(lines)}
        onComplete={() => {
          if (terminationStatus == undefined) {
            onError("Unexpected close of subscription connection")
          }
        }}
      /> : null}
  </>
})