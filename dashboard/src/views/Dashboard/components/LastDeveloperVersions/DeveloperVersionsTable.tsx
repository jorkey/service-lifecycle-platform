import React, {useState} from 'react';
import clsx from 'clsx';
import { makeStyles } from '@material-ui/styles';
import {
  Card,
  CardContent, CardHeader,
} from '@material-ui/core';
import {
  ClientVersionsInfoQuery, DeveloperVersionsInfoQuery,
  useDeveloperVersionsInfoQuery,
} from "../../../../generated/graphql";
import GridTable from "../../../../common/components/gridTable/GridTable";
import {Version} from "../../../../common";
import Alert from "@material-ui/lab/Alert";
import FormGroup from "@material-ui/core/FormGroup";
import {RefreshControl} from "../../../../common/components/refreshControl/RefreshControl";
import {GridTableColumnParams, GridTableColumnValue} from "../../../../common/components/gridTable/GridTableColumn";
import {GridTableRowParams} from "../../../../common/components/gridTable/GridTableRow";

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
    padding: '4px',
    paddingLeft: '16px'
  },
  versionColumn: {
    width: '150px',
    padding: '4px',
  },
  authorColumn: {
    width: '150px',
    padding: '4px',
  },
  commentColumn: {
    padding: '4px',
  },
  creationTime: {
    width: '250px',
    padding: '4px',
  },
  control: {
    paddingLeft: '10px',
    textTransform: 'none'
  },
  alert: {
    marginTop: 25
  }
}));

interface LastDeveloperVersionsTableProps {
  developerVersions:  DeveloperVersionsInfoQuery | undefined
}

const LastDeveloperVersionsTable: React.FC<LastDeveloperVersionsTableProps> = (props) => {
  const { developerVersions } = props

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
  ]

  const rows = developerVersions?.developerVersionsInfo
    .sort((v1, v2) =>
      Version.compareBuilds(v2.version.build, v1.version.build))
    .map(version => ({
      columnValues: new Map<string, GridTableColumnValue>([
        ['service', version.service],
        ['version', Version.buildToString(version.version.build)],
        ['author', version.buildInfo.author],
        ['comment', version.buildInfo.comment?version.buildInfo.comment:''],
        ['creationTime', version.buildInfo.time]
      ]) } as GridTableRowParams))

  return <GridTable
           className={classes.versionsTable}
           columns={columns}
           rows={rows?rows:[]}/>
}

export default LastDeveloperVersionsTable