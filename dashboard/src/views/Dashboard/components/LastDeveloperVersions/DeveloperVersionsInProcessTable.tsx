import React from 'react';
import { makeStyles } from '@material-ui/styles';
import GridTable from "../../../../common/components/gridTable/GridTable";
import {GridTableColumnParams} from "../../../../common/components/gridTable/GridTableColumn";
import {TasksQuery} from "../../../../generated/graphql";
import {GridTableCellParams} from "../../../../common/components/gridTable/GridTableCell";

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
  serviceColumn: {
    width: '150px',
  },
  versionColumn: {
    width: '150px',
  },
  authorColumn: {
    width: '150px',
  },
  commentColumn: {
  },
  startTimeColumn: {
    width: '150px',
  },
  control: {
    paddingLeft: '10px',
    textTransform: 'none'
  },
  alert: {
    marginTop: 25
  },
}));

interface DeveloperVersionsInProcessTableProps {
  developerVersionsInProcess: TasksQuery | undefined
}

const DeveloperVersionsInProcessTable: React.FC<DeveloperVersionsInProcessTableProps> = (props) => {
  const { developerVersionsInProcess } = props

  const classes = useStyles()

  const columns: Array<GridTableColumnParams> = [
    {
      name: 'service',
      headerName: 'Service',
      className: classes.serviceColumn,
    },
    {
      name: 'version',
      headerName: 'Version In Process',
      className: classes.versionColumn,
    },
    {
      name: 'author',
      headerName: 'Author',
      className: classes.authorColumn,
    },
    {
      name: 'startTime',
      type: 'date',
      headerName: 'Start Time',
      className: classes.startTimeColumn,
    },
    {
      name: 'comment',
      headerName: 'Comment',
      className: classes.commentColumn,
    }
  ]

  const rows = developerVersionsInProcess?.tasks.map(task => (
    new Map<string, GridTableCellParams>([
      ['service', { value: task.parameters.find(p => p.name == 'service')?.value }],
      ['version', { value: task.parameters.find(p => p.name == 'version')?.value }],
      ['author', { value: task.parameters.find(p => p.name == 'author')?.value }],
      ['comment', { value: task.parameters.find(p => p.name == 'comment')?.value }],
      ['startTime', { value: task.creationTime }]
    ])))

  return rows?.length?<GridTable className={classes.versionsTable}
           columns={columns}
           rows={rows?rows:[]}/>:null
}

export default DeveloperVersionsInProcessTable