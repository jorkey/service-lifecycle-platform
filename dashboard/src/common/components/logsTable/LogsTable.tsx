import React, {useEffect, useState} from "react";
import {
  LogLine,
  useDirectoryLogsLazyQuery, useInstanceLogsLazyQuery,
  useProcessLogsLazyQuery, useServiceLogsLazyQuery,
} from "../../../generated/graphql";
import GridTable from "../gridTable/GridTable";
import {makeStyles} from "@material-ui/core/styles";
import {GridTableColumnParams, GridTableColumnValue} from "../gridTable/GridTableColumn";
import {LogsSubscriber} from "./LogsSubscriber";
import {subscribe} from "graphql";

const useStyles = makeStyles(theme => ({
  div: {
    display: 'relative'
  },
  timeColumn: {
    width: '200px',
    minWidth: '200px',
    padding: '4px',
    paddingLeft: '16px'
  },
  instanceColumn: {
    width: '200px',
    minWidth: '200px',
    padding: '4px',
    paddingLeft: '16px'
  },
  processColumn: {
    width: '100px',
    minWidth: '100px',
    padding: '4px',
    paddingLeft: '16px'
  },
  levelColumn: {
    width: '100px',
    minWidth: '100px',
    padding: '4px',
    paddingLeft: '16px'
  },
  messageColumn: {
    whiteSpace: 'pre',
    padding: '4px',
    paddingLeft: '16px'
  },
  inProgress: {
    cursor: 'progress',
  }
}))

export interface FindLogsDashboardParams {
  className: string
  service?: string
  instance?: string
  process?: string
  directory?: string
  task?: string
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

export const LogsTable = (props: LogsTableParams) => {
  const { className, service, instance, process, directory, task, fromTime, toTime, levels, find,
    follow, onComplete, onError } = props

  const [ lines, setLines ] = useState<LogRecord[]>([])

  const [ terminationStatus, setTerminationStatus ] = useState<boolean>()

  const sliceRowsCount = 50

  const [ getServiceLogs, serviceLogs ] = useServiceLogsLazyQuery({
    fetchPolicy: 'no-cache',
    onError(err) { onError(err.message) },
    onCompleted(data) { if (data.logs) { addLines(data.logs) } }
  })

  const [ getInstanceLogs, instanceLogs ] = useInstanceLogsLazyQuery({
    fetchPolicy: 'no-cache',
    onError(err) { onError(err.message) },
    onCompleted(data) { if (data.logs) { addLines(data.logs) } }
  })

  const [ getDirectoryLogs, directoryLogs ] = useDirectoryLogsLazyQuery({
    fetchPolicy: 'no-cache',
    onError(err) { onError(err.message) },
    onCompleted(data) { if (data.logs) { addLines(data.logs) } }
  })

  const [ getProcessLogs, processLogs ] = useProcessLogsLazyQuery({
    fetchPolicy: 'no-cache',
    onError(err) { onError(err.message) },
    onCompleted(data) { if (data.logs) { addLines(data.logs) } }
  })

  const isLoading = () => serviceLogs.loading || instanceLogs.loading || directoryLogs.loading || processLogs.loading

  const getCommonVariables = (from?: BigInt, to?: BigInt) => {
    return {
      fromTime: fromTime, toTime: toTime, levels: levels, find: find,
      from: from, to: to, limit: sliceRowsCount
    }
  }

  const getLogs = (from?: BigInt, to?: BigInt) => {
    if (service && instance && directory && process) {
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
  ].filter(column => column.name != 'instance' || (!instance && !task))
   .filter(column => column.name != 'process' || (!process && !task)) as GridTableColumnParams[]

  const rows = lines
    .map(line => {
      return new Map<string, GridTableColumnValue>([
        ['time', line.payload.time],
        ['level', line.payload.level],
        ['instance', line.instance?line.instance:''],
        ['process', line.process?line.process:''],
        ['level', line.payload.level],
        ['unit', line.payload.unit],
        ['message', line.payload.message]
      ]) })

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
  }

  return <>
    <GridTable
      className={className + (isLoading() ? ' ' + classes.inProgress : '')}
      columns={columns}
      rows={rows}
      scrollToLastRow={follow}
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
}