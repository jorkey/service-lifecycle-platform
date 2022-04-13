import React, {useEffect, useState} from "react";
import {
  SequencedTaskInfo,
  TaskParameter, useTaskLogsLazyQuery, useTasksLazyQuery,
  useTasksQuery,
} from "../../../generated/graphql";
import GridTable from "../gridTable/GridTable";
import {makeStyles} from "@material-ui/core/styles";
import {GridTableColumnParams} from "../gridTable/GridTableColumn";
import {GridTableCellParams} from "../gridTable/GridTableCell";
import {LogRecord} from "../logsTable/LogsTable";

const useStyles = makeStyles(theme => ({
  div: {
    display: 'relative'
  },
  timeColumn: {
    width: '200px',
    paddingLeft: 4
  },
  idColumn: {
    width: '100px',
  },
  typeColumn: {
    width: '100px',
  },
  parametersColumn: {
  },
  statusColumn: {
    width: '200px',
  },
  inProgress: {
    cursor: 'progress',
  }
}))

interface TasksTableParams {
  className: string
  type: string | undefined
  service: string | undefined
  onlyActive: boolean | undefined
  fromTime: Date | undefined
  toTime: Date | undefined
  onClick: (id: string) => void
  onError: (msg: string) => void
}

export const TasksTable = (props: TasksTableParams) => {
  const { className, type, service, onlyActive, fromTime, toTime, onClick, onError } = props

  const [ tasks, setTasks ] = useState<SequencedTaskInfo[]>([])

  const [ getTasksRequest, tasksRequest ] = useTasksLazyQuery({
    fetchPolicy: 'no-cache', // base option no-cache does not work
    onCompleted(data) { if (data.tasks) { addTasks(data.tasks as SequencedTaskInfo[]) } },
    onError(err) { onError('Query tasks error ' + err.message) }
  })

  useEffect(() => {
    setTasks([])
    getTasks()
  },  [ type, service, onlyActive, fromTime, toTime ])

  const classes = useStyles()

  const columns: GridTableColumnParams[] = [
    {
      name: 'creationTime',
      headerName: 'Creation Time',
      className: classes.timeColumn,
      type: 'time',
    },
    {
      name: 'task',
      headerName: 'TaskId',
      className: classes.idColumn
    },
    {
      name: 'type',
      headerName: 'Type',
      className: classes.typeColumn
    },
    {
      name: 'parameters',
      headerName: 'Parameters',
      className: classes.parametersColumn
    },
    {
      name: 'terminationTime',
      headerName: 'Termination Time',
      className: classes.timeColumn,
      type: 'time'
    },
    {
      name: 'terminationStatus',
      headerName: 'Termination Status',
      className: classes.statusColumn,
    }
  ].filter(column => column.name != 'active' || !onlyActive) as GridTableColumnParams[]

  function parametersToString(params: TaskParameter[]) {
    let str = ''
    params.forEach(p => {
      if (str) {
        str += '\n'
      }
      str += `${p.name}:${p.value}`
    })
    return str
  }

  const addTasks = (receivedTasks: SequencedTaskInfo[]) => {
    const end = tasks.length ? tasks[tasks.length-1].sequence : BigInt(0)
    const append = receivedTasks.filter(line => line.sequence > end)
    setTasks(new Array(...tasks, ...append))
  }

  const rows = tasks
    .map(task => new Map<string, GridTableCellParams>([
        ['creationTime', { value: task.creationTime }],
        ['task', { value: task.task }],
        ['type', { value: task.type }],
        ['parameters', { value: parametersToString(task.parameters) }],
        ['terminationTime', { value: task.terminationTime?task.terminationTime:undefined }],
        ['terminationStatus', { value: task.terminationStatus == undefined?'':task.terminationStatus?'Success':'Failure' }]
      ]))

  const getTasks = (from?: BigInt) => {
    getTasksRequest({variables: { type: type, service: service, onlyActive: onlyActive, fromTime: fromTime, toTime: toTime, from: from, limit: 100 }})
  }

  const isLoading = () => tasksRequest.loading

  return rows?<>
    <GridTable
      className={className + (isLoading() ? ' ' + classes.inProgress : '')}
      columns={columns}
      rows={rows}
      onScrollBottom={() => {
        if (tasks.length) {
          getTasks(tasks[tasks.length-1].sequence)
        }
      }}
      onClick={row =>
        onClick(rows[row].get('task')?.value! as string)
      }
    />
  </>:null
}