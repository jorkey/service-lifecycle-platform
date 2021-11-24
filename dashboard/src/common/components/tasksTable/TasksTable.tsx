import React from "react";
import {
  TaskInfo, TaskParameter,
  useTasksQuery,
} from "../../../generated/graphql";
import GridTable from "../gridTable/GridTable";
import {makeStyles} from "@material-ui/core/styles";
import {GridTableColumnParams, GridTableCellParams} from "../gridTable/GridTableColumn";
import {GridTableRowParams} from "../gridTable/GridTableRow";

const useStyles = makeStyles(theme => ({
  div: {
    display: 'relative'
  },
  creationTimeColumn: {
    width: '200px',
    padding: '4px',
    paddingLeft: '16px'
  },
  idColumn: {
    width: '100px',
    padding: '4px',
    paddingLeft: '16px'
  },
  typeColumn: {
    width: '100px',
    padding: '4px',
    paddingLeft: '16px'
  },
  parametersColumn: {
    padding: '4px',
    paddingLeft: '16px'
  },
  activeColumn: {
    whiteSpace: 'pre',
    width: '100px',
    padding: '4px',
    paddingLeft: '16px'
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
      name: 'id',
      headerName: 'ID',
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
    .map(task => ({
      columnValues: new Map<string, GridTableCellParams>([
        ['creationTime', task.creationTime],
        ['id', task.id],
        ['type', task.type],
        ['parameters', parametersToString(task.parameters)],
        ['active', task.active?true:false]
      ]) }) as GridTableRowParams)

  return rows?<>
    <GridTable
      className={className}
      columns={columns}
      rows={rows}
      onClick={row => onClick(rows[row].columnValues.get('id')! as string)}
    />
  </>:null
}