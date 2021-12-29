import React from "react";
import {
  TaskParameter,
  useTasksQuery,
} from "../../../generated/graphql";
import GridTable from "../gridTable/GridTable";
import {makeStyles} from "@material-ui/core/styles";
import {GridTableColumnParams} from "../gridTable/GridTableColumn";
import {GridTableCellParams} from "../gridTable/GridTableCell";

const useStyles = makeStyles(theme => ({
  div: {
    display: 'relative'
  },
  creationTimeColumn: {
    width: '200px',
  },
  idColumn: {
    width: '100px',
  },
  typeColumn: {
    width: '100px',
  },
  parametersColumn: {
  },
  activeColumn: {
    whiteSpace: 'pre',
    width: '100px',
  }
}))

interface TasksTableParams {
  className: string
  type: string | undefined
  onlyActive: boolean | undefined
  onClick: (id: string) => void
  onError: (msg: string) => void
}

export const TasksTable = (props: TasksTableParams) => {
  const { className, type, onlyActive, onClick, onError } = props

  const { data: tasks } = useTasksQuery({
    fetchPolicy: 'no-cache', // base option no-cache does not work
    variables: { type: type, onlyActive: onlyActive },
    onError(err) { onError('Query tasks error ' + err.message) },
  })

  const classes = useStyles()

  const columns: GridTableColumnParams[] = [
    {
      name: 'creationTime',
      headerName: 'Creation Time',
      className: classes.creationTimeColumn,
      type: 'date',
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
      name: 'active',
      headerName: 'Active',
      className: classes.activeColumn
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

  const rows = tasks?.tasks
    .map(task => new Map<string, GridTableCellParams>([
        ['creationTime', { value: task.creationTime }],
        ['task', { value: task.task }],
        ['type', { value: task.type }],
        ['parameters', { value: parametersToString(task.parameters) }],
        ['active', { value: task.active?true:false }]
      ]))

  return rows?<>
    <GridTable
      className={className}
      columns={columns}
      rows={rows}
      onClick={row => onClick(rows[row].get('task')?.value! as string)}
    />
  </>:null
}