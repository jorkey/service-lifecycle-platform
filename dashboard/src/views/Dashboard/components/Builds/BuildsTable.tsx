import React, {useCallback} from 'react';
import { makeStyles } from '@material-ui/styles';
import GridTable from "../../../../common/components/gridTable/GridTable";
import {GridTableColumnParams} from "../../../../common/components/gridTable/GridTableColumn";
import {BuildStatus, TasksQuery, TimedBuildServiceState} from "../../../../generated/graphql";
import {GridTableCellParams} from "../../../../common/components/gridTable/GridTableCell";
import {useHistory, useRouteMatch} from "react-router-dom";
import {InputLabel, Link} from "@material-ui/core";

const useStyles = makeStyles((theme:any) => ({
  root: {},
  content: {
    padding: 0
  },
  inner: {
    minWidth: 800
  },
  versionsTable: {
    marginTop: 20,
    maxHeight: 250
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
  targetColumn: {
    width: '50px',
  },
  statusColumn: {
    width: '40px',
    paddingRight: '10px'
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

  const history = useHistory()

  const handleOnClick = useCallback((task: string) => {
    history.push('/logging/tasks/' + task)
  }, [ history ]);

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
      name: 'target',
      headerName: 'Target',
      className: classes.targetColumn,
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
      ['target', { value: state.target.toString() }],
      ['status', { value: [
          <InputLabel style={{color:
              state.status==BuildStatus.InProcess?'blue':state.status==BuildStatus.Success?'green':'red', paddingBottom: '4px'}}>
            {state.status.toString()}
          </InputLabel>
      ] }],
    ])))

  return rows?.length?<GridTable className={classes.versionsTable}
           columns={columns}
           rows={rows?rows:[]}
           onClicked={row => handleOnClick(buildStates![row]?.task)}
  />:null
}
export default BuildsTable