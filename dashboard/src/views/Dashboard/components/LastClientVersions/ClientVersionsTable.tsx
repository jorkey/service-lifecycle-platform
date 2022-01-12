import React from 'react';
import { makeStyles } from '@material-ui/styles';
import { ClientVersionsInfoQuery } from "../../../../generated/graphql";
import GridTable from "../../../../common/components/gridTable/GridTable";
import {Version} from "../../../../common";
import {GridTableColumnParams} from "../../../../common/components/gridTable/GridTableColumn";
import {GridTableCellParams} from "../../../../common/components/gridTable/GridTableCell";

const useStyles = makeStyles((theme:any) => ({
  root: {},
  content: {
    padding: 0
  },
  versionsTable: {
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
  creationTime: {
    width: '250px',
  },
  installedByColumn: {
    width: '150px',
  },
  installTimeColumn: {
    width: '250px',
  },
  control: {
    paddingLeft: '10px',
    textTransform: 'none'
  },
  alert: {
    marginTop: 25
  }
}));

interface LastClientVersionsTableProps {
  clientVersions:  ClientVersionsInfoQuery | undefined
}

const ClientVersionsTable: React.FC<LastClientVersionsTableProps> = (props) => {
  const { clientVersions } = props

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
      name: 'creationTime',
      headerName: 'Creation Time',
      type: 'date',
      className: classes.creationTime,
    },
    {
      name: 'comment',
      headerName: 'Comment',
      className: classes.commentColumn,
    },
    {
      name: 'installedBy',
      headerName: 'Installed By',
      className: classes.installedByColumn,
    },
    {
      name: 'installTime',
      headerName: 'Install Time',
      type: 'date',
      className: classes.installTimeColumn,
    }
  ]

  const rows = clientVersions?.clientVersionsInfo?[...clientVersions?.clientVersionsInfo]
    .sort((v1, v2) =>
      v1.installInfo.time.getTime() > v2.installInfo.time.getTime() ? -1 :
      v1.installInfo.time.getTime() < v2.installInfo.time.getTime() ? 1 : 0)
    .slice(0, 5)
    .map(version => (
      new Map<string, GridTableCellParams>([
        ['service', { value: version.service }],
        ['version', { value: Version.clientDistributionVersionToString(version.version) }],
        ['author', { value: version.buildInfo.author }],
        ['comment', { value: version.buildInfo.comment?version.buildInfo.comment:'' }],
        ['creationTime', { value: version.buildInfo.time }],
        ['installedBy', { value: version.installInfo.account }],
        ['installTime', { value: version.installInfo.time }],
      ]))):undefined

  return <GridTable className={classes.versionsTable}
                    columns={columns} rows={rows?rows:[]}/>
}

export default ClientVersionsTable