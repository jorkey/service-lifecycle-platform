import React from 'react';
import { makeStyles } from '@material-ui/styles';
import {GridTableColumnParams, GridTableCellParams} from "../../../../common/components/gridTable/GridTableColumn";
import GridTable from "../../../../common/components/gridTable/GridTable";
import {TasksQuery} from "../../../../generated/graphql";

const useStyles = makeStyles((theme:any) => ({
  root: {},
  content: {
    padding: 0
  },
  inner: {
    minWidth: 800
  },
  versionsTable: {
    marginTop: 20
  },
  versionsColumn: {
  },
  authorColumn: {
    width: '150px',
  },
  creationTime: {
    width: '250px',
  },
  control: {
    textTransform: 'none'
  },
  alert: {
    marginTop: 25
  },
}));

interface ClientVersionsInProcessTableProps {
  clientVersionsInProcess: TasksQuery | undefined
}

const ClientVersionsInProcessTable: React.FC<ClientVersionsInProcessTableProps> = (props) => {
  const { clientVersionsInProcess } = props

  const classes = useStyles()

  const columns: Array<GridTableColumnParams> = [
    {
      name: 'versions',
      headerName: 'Versions In Process',
      className: classes.versionsColumn,
    },
    {
      name: 'author',
      headerName: 'Author',
      className: classes.authorColumn,
    },
    {
      name: 'startTime',
      headerName: 'Start',
      type: 'date',
      className: classes.creationTime,
    },
  ]

  const rows = clientVersionsInProcess?.tasks
    .map(task => (
      new Map<string, GridTableCellParams>([
        ['versions', { value: task.parameters.find(p => p.name == 'versions')?.value }],
        ['author', { value: task.parameters.find(p => p.name == 'author')?.value }],
        ['startTime', { value: task.creationTime }],
      ])))

  return rows?.length?<GridTable className={classes.versionsTable}
                       columns={columns} rows={rows}/>:null
}

export default ClientVersionsInProcessTable