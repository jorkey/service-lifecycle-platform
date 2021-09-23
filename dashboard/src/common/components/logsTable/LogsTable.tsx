import React, {useEffect, useState} from "react";
import {
  SequencedServiceLogLine,
  useLogsLazyQuery, useLogsQuery,
} from "../../../generated/graphql";
import GridTable from "../gridTable/GridTable";
import {makeStyles} from "@material-ui/core/styles";
import {GridTableColumnParams, GridTableColumnValue} from "../gridTable/GridTableColumn";
import {LogsSubscriber} from "./LogsSubscriber";

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
  level?: string
  find?: string
  follow?: boolean
}

interface LogsTableParams extends FindLogsDashboardParams {
  onComplete: (time: Date, status: boolean) => void
  onError: (message: string) => void
}

export const LogsTable = (props: LogsTableParams) => {
  const { className, service, instance, process, directory, task, fromTime, toTime, level, find,
    follow, onComplete, onError } = props

  const [ lines, setLines ] = useState<SequencedServiceLogLine[]>([])

  const [ terminationStatus, setTerminationStatus ] = useState<boolean>()
  const [ subscribeFrom, setSubscribeFrom ] = useState<BigInt>()

  const sliceRowsCount = 50
  const maxRowsCount = 100

  const getLogsVariables = (from?: BigInt, to?: BigInt) => {
    return {
      service: service, instance: instance, process: process, directory: directory, task: task,
      fromTime: fromTime, toTime: toTime, level: level, find: find,
      from: from, to: to, limit: sliceRowsCount
    }
  }

  const [ getLogs, logs ] = useLogsLazyQuery({
    fetchPolicy: 'no-cache',
    onError(err) {
      onError(err.message)
    },
    onCompleted() {
      if (logs.data?.logs) {
        addLines(logs.data.logs)
      }
    }
  })

  useEffect(() => {
      setLines([])
      getLogs({ variables: getLogsVariables(
          follow?undefined:BigInt(0),
          follow?BigInt('9223372036854775807'):undefined) })
    },
    [ service, instance, process, directory, task, fromTime, toTime, level, find, follow ])

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
  ]

  const rows = lines
      .map(line => line.line)
      .map(line => new Map<string, GridTableColumnValue>([
    ['time', line.time],
    ['level', line.level],
    ['unit', line.unit],
    ['message', line.message]
  ]))

  const addLines = (receivedLines: SequencedServiceLogLine[]) => {
    const begin = lines.length ? lines[0].sequence : BigInt(0)
    const insert = receivedLines.filter(line => line.sequence < begin)
    let newLines = new Array(...lines)
    if (insert.length) {
      newLines = new Array(...insert, ...lines)
      // if (!subscribe && newLines.length > maxRowsCount) {
      //   newLines = newLines.slice(0, maxRowsCount)
      // }
    }
    const end = lines.length ? lines[lines.length-1].sequence : BigInt(0)
    const append = receivedLines.filter(line => line.sequence > end)
    if (append.length) {
      newLines = new Array(...newLines, ...append)
      // if (newLines.length > maxRowsCount) {
      //   newLines = newLines.slice(newLines.length - maxRowsCount)
      // }
    }
    setLines(newLines)
    if (newLines.length) {
      const status = newLines[newLines.length-1].line.terminationStatus
      if (status != undefined) {
        setTerminationStatus(status)
        onComplete(newLines[0].line.time, status)
      }
    }
  }

  if (follow && subscribeFrom == undefined && terminationStatus == undefined && !logs.loading) {
    setSubscribeFrom(lines.length?lines[lines.length-1].sequence:BigInt(0))
  }

  return <>
    <GridTable
      className={className + (logs.loading ? ' ' + classes.inProgress : '')}
      columns={columns}
      rows={rows}
      scrollToLastRow={follow}
      onScrollTop={() => {
        if (lines.length) {
          getLogs({ variables: getLogsVariables(undefined, lines[0].sequence)})
        }
      }}
      onScrollBottom={() => {
        if (!follow && lines.length && lines[lines.length - 1].line.terminationStatus == undefined) {
          getLogs({ variables: getLogsVariables(lines[lines.length - 1].sequence, undefined) })
        }
      }}
    />
    {subscribeFrom != undefined ?
      <LogsSubscriber
        {...props}
        from={subscribeFrom}
        onLine={line => addLines([line])}
        onComplete={() => {
          setSubscribeFrom(undefined)
          if (terminationStatus == undefined) {
            onError("Unexpected close of subscription connection")
          }
        }}
      /> : null}
  </>
}