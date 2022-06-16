import React from 'react';
import { makeStyles } from '@material-ui/styles';
import GridTable from "../../../../common/components/gridTable/GridTable";
import {GridTableColumnParams} from "../../../../common/components/gridTable/GridTableColumn";
import {TasksQuery, TimedBuildServiceState} from "../../../../generated/graphql";
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
  statusColumn: {
  },
  startTimeColumn: {
    width: '250px',
  },
  control: {
    paddingLeft: '10px',
  },
  alert: {
    marginTop: 25
  },
}));

interface BuildsTableProps {
  buildStates: TimedBuildServiceState[] | undefined
}

const BuildsTable: React.FC<BuildsTableProps> = (props) => {
  const { buildStates } = props

  const classes = useStyles()

  const columns: Array<GridTableColumnParams> = [
    {
      name: 'service',
      headerName: 'Service',
      className: classes.serviceColumn,
    },
    {
      name: 'version',
      headerName: 'Version',
      className: classes.versionColumn,
    },
    {
      name: 'author',
      headerName: 'Author',
      className: classes.authorColumn,
    },
    {
      name: 'time',
      type: 'relativeTime',
      headerName: 'Time',
      className: classes.startTimeColumn,
    },
    {
      name: 'comment',
      headerName: 'Comment',
      className: classes.commentColumn,
    },
    {
      name: 'status',
      headerName: 'Status',
      className: classes.statusColumn,
    }
  ]

  const rows = buildStates?.map(state => (
    new Map<string, GridTableCellParams>([
      ['service', { value: state.service }],
      ['version', { value: state.version }],
      ['author', { value: state.author }],
      ['time', { value: state.time }],
      ['comment', { value: state.comment }],
      ['status', { value: state.status.toString() }],
    ])))

  return rows?.length?<GridTable className={classes.versionsTable}
           columns={columns}
           rows={rows?rows:[]}/>:null
}

export default BuildsTable