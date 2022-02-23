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
import {GridTableColumnParams} from "../../../../common/components/gridTable/GridTableColumn";
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
  control: {
    paddingLeft: '10px',
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

  const rows = (developerVersions?.developerVersionsInfo ? [...developerVersions?.developerVersionsInfo] : [])
    .sort((v1, v2) =>
      v1.buildInfo.time.getTime() > v2.buildInfo.time.getTime() ? -1 :
      v1.buildInfo.time.getTime() < v2.buildInfo.time.getTime() ? 1 : 0)
    .slice(0, 5)
    .map(version => (
      new Map<string, GridTableCellParams>([
        ['service', { value: version.service }],
        ['version', { value: Version.buildToString(version.version.build) }],
        ['author', { value: version.buildInfo.author }],
        ['comment', { value: version.buildInfo.comment?version.buildInfo.comment:'' }],
        ['creationTime', { value: version.buildInfo.time }]
      ])))

  return <GridTable
           className={classes.versionsTable}
           columns={columns}
           rows={rows?rows:[]}/>
}

export default LastDeveloperVersionsTable