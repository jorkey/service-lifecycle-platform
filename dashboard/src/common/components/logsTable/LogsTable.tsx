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
    paddingLeft: 4,
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
  unitColumn: {
    minWidth: '100px',
    maxWidth: '100px',
    textOverflow: 'ellipsis'
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

  const [ from, setFrom ] = useState<BigInt | undefined>()
  const [ to, setTo ] = useState<BigInt | undefined>()

  const [ terminationStatus, setTerminationStatus ] = useState<boolean>()

  const startSequence = BigInt(0)
  const endSequence = BigInt('0x7FFFFFFFFFFFFFFF')

  useImperativeHandle(ref, () => ({
    toTop: () => {
      if (from != startSequence) {
        setLines([])
        setFrom(startSequence)
        setTo(undefined)
      }
    },
    toBottom: () => {
      if (to != endSequence) {
        setLines([])
        setFrom(undefined)
        setTo(endSequence)
      }
    }
  }))

  const sliceRowsCount = 250

  const [ getTaskLogs, taskLogs ] = useTaskLogsLazyQuery({
    fetchPolicy: 'no-cache', // base option no-cache does not work
    onError(err) {
      onError(err.message) }
    ,
    onCompleted(data) {
      if (data.logs) { addLines(data.logs) }
    }
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

  const getCommonVariables = () => {
    return {
      fromTime: fromTime, toTime: toTime, levels: levels, find: find,
      from: from, to: to, limit: sliceRowsCount
    }
  }

  useEffect(() => {
    setLines([])
    if (!follow) {
      setFrom(startSequence)
    }
  },  [ service, instance, directory, process, task, fromTime, toTime, levels, find, follow ])

  useEffect(() => {
    if (task) {
      taskLogs.previousData = undefined
      getTaskLogs({variables: {task: task, ...getCommonVariables()}})
    } else if (service && instance && directory && process) {
      getProcessLogs({ variables: { service: service, instance: instance, directory: directory, process: process, ...getCommonVariables() }  })
    } else if (service && instance && directory) {
      getDirectoryLogs({ variables: { service: service, instance: instance, directory: directory, ...getCommonVariables() }  })
    } else if (service && instance) {
      getInstanceLogs({ variables: { service: service, instance: instance, ...getCommonVariables() }  })
    } else if (service) {
      getServiceLogs({ variables: { service: service, ...getCommonVariables() }  })
    }
  },  [ service, instance, directory, process, task, fromTime, toTime, levels, find, follow, from, to ])

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
    // { TODO
    //   name: 'unit',
    //   headerName: 'Unit',
    //   className: classes.unitColumn
    // },
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
      scrollToRow={
        ((from == undefined && to == endSequence) || follow) ? rows.length-1 :
        (from == startSequence && endSequence == undefined) ? 0 : undefined
      }
      onScrollTop={() => {
        if (lines.length) {
          setFrom(undefined)
          setTo(lines[0].sequence)
        }
      }}
      onScrollBottom={() => {
        if (!follow && lines.length && lines[lines.length - 1].payload.terminationStatus == undefined) {
          setFrom(lines[lines.length - 1].sequence)
          setTo(undefined)
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