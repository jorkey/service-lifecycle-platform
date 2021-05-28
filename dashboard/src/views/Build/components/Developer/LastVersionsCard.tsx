import React, {useState} from 'react';
import clsx from 'clsx';
import { makeStyles } from '@material-ui/styles';
import {
  Card,
  CardContent, CardHeader,
} from '@material-ui/core';
import {useDeveloperVersionsInfoQuery} from "../../../../generated/graphql";
import Grid, {GridColumnValue, GridColumnParams} from "../../../../common/Grid";
import {Version} from "../../../../common";

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
    width: '250px',
    padding: '4px',
    paddingLeft: '16px'
  },
  versionColumn: {
    width: '250px',
    padding: '4px',
    paddingLeft: '16px'
  },
  authorColumn: {
    width: '250px',
    padding: '4px',
    paddingLeft: '16px'
  },
  commentColumn: {
    padding: '4px',
    paddingLeft: '16px'
  },
  taskColumn: {
    width: '250px',
    padding: '4px',
    paddingLeft: '16px'
  },
  startTimeColumn: {
    width: '250px',
    padding: '4px',
    paddingLeft: '16px'
  },
}));

const LastVersionsCard = () => {
  const classes = useStyles()

  const {data:developerVersionsInfo} = useDeveloperVersionsInfoQuery()

  const columns: Array<GridColumnParams> = [
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
      name: 'comment',
      headerName: 'Comment',
      className: classes.commentColumn,
    },
    {
      name: 'task',
      headerName: 'Task',
      className: classes.taskColumn,
    },
    {
      name: 'creationTime',
      headerName: 'Creation Time',
      className: classes.startTimeColumn,
    },
  ]

  const rows = developerVersionsInfo?.developerVersionsInfo.map(
      version => new Map<string, GridColumnValue>([
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
          <Grid
           className={classes.versionsTable}
           columns={columns}
           rows={rows?rows:[]}/>
        </div>
      </CardContent>
    </Card>
  );
}

export default LastVersionsCard