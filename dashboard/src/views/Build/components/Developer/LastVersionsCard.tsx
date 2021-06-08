import React, {useState} from 'react';
import clsx from 'clsx';
import { makeStyles } from '@material-ui/styles';
import {
  Card,
  CardContent, CardHeader,
} from '@material-ui/core';
import {useDeveloperVersionsInfoQuery} from "../../../../generated/graphql";
import GridTable from "../../../../common/grid/GridTable";
import {Version} from "../../../../common";
import {GridTableColumnParams, GridTableColumnValue} from "../../../../common/grid/GridTableRow";
import {instanceOf} from "prop-types";

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
    width: '200px',
    padding: '4px',
    paddingLeft: '16px'
  },
  versionColumn: {
    width: '200px',
    padding: '4px',
    paddingLeft: '16px'
  },
  authorColumn: {
    width: '200px',
    padding: '4px',
    paddingLeft: '16px'
  },
  commentColumn: {
    padding: '4px',
    paddingLeft: '16px'
  },
  taskColumn: {
    width: '100px',
    padding: '4px',
    paddingLeft: '16px'
  },
  startTimeColumn: {
    width: '200px',
    padding: '4px',
    paddingLeft: '16px'
  },
}));

const LastVersionsCard = () => {
  const classes = useStyles()

  const {data:developerVersionsInfo} = useDeveloperVersionsInfoQuery()

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
      className: classes.startTimeColumn,
    },
    {
      name: 'comment',
      headerName: 'Comment',
      className: classes.commentColumn,
    },
  ]

  const rows = developerVersionsInfo?.developerVersionsInfo.map(
      version => new Map<string, GridTableColumnValue>([
    ['service', version.service],
    ['version', Version.buildToString(version.version.build)],
    ['author', version.buildInfo.author],
    ['comment', version.buildInfo.comment?version.buildInfo.comment:''],
    ['creationTime', version.buildInfo.date]
  ]))

  return (
    <Card
      className={clsx(classes.root)}
    >
      <CardHeader title='Last Versions'/>
      <CardContent className={classes.content}>
        <div className={classes.inner}>
          <GridTable
           className={classes.versionsTable}
           columns={columns}
           rows={rows?rows:[]}/>
        </div>
      </CardContent>
    </Card>
  );
}

export default LastVersionsCard