import React from "react";
import {
  TaskInfo,
  useTasksQuery,
} from "../../../generated/graphql";
import GridTable from "../gridTable/GridTable";
import {makeStyles} from "@material-ui/core/styles";
import {GridTableColumnParams, GridTableColumnValue} from "../gridTable/GridTableColumn";

const useStyles = makeStyles(theme => ({
  div: {
    display: 'relative'
  },
  creationTimeColumn: {
    width: '100px',
    minWidth: '100px',
    padding: '4px',
    paddingLeft: '16px'
  },
  idColumn: {
    width: '100px',
    minWidth: '100px',
    padding: '4px',
    paddingLeft: '16px'
  },
  typeColumn: {
    width: '100px',
    minWidth: '100px',
    padding: '4px',
    paddingLeft: '16px'
  },
  parametersColumn: {
    width: '400px',
    minWidth: '400px',
    padding: '4px',
    paddingLeft: '16px'
  },
  activeColumn: {
    whiteSpace: 'pre',
    padding: '4px',
    paddingLeft: '16px'
  }
}))

interface TasksTableParams {
  className: string
  taskType: string | undefined
  onlyActive: boolean | undefined
  onClick: (id: string) => void
  onError: (msg: string) => void
}

export const TasksTable = (props: TasksTableParams) => {
  const { className, taskType, onlyActive, onClick, onError } = props

  const { data: tasks } = useTasksQuery({
    fetchPolicy: 'no-cache',
    variables: { taskType: taskType, onlyActive: onlyActive },
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
  ].filter(column => column.name != 'active' || onlyActive) as GridTableColumnParams[]

  const rows = tasks?.tasks
    .map(task => {
      return new Map<string, GridTableColumnValue>([
        ['creationTime', task.creationTime],
        ['id', task.id],
        ['type', task.taskType],
        ['parameters', task.parameters.toString()],
        ['active', task.active?true:false]
      ]) })

  return rows?<>
    <GridTable
      className={className}
      columns={columns}
      rows={rows}
      onClick={row => onClick(rows[row].get('id')! as string)}
    />
  </>:null
}