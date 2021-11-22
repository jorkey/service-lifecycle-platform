import React, {useState} from 'react';
import clsx from 'clsx';
import { makeStyles } from '@material-ui/styles';
import {
  Card, CardContent, CardHeader, Grid, Typography,
} from '@material-ui/core';
import Alert from "@material-ui/lab/Alert";
import FormGroup from "@material-ui/core/FormGroup";
import {RefreshControl} from "../../../../common/components/refreshControl/RefreshControl";
import {TasksQuery, useTasksQuery} from "../../../../generated/graphql";
import {GridTableColumnParams, GridTableColumnValue} from "../../../../common/components/gridTable/GridTableColumn";
import {GridTableRowParams} from "../../../../common/components/gridTable/GridTableRow";
import GridTable from "../../../../common/components/gridTable/GridTable";

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
    padding: '4px',
    paddingLeft: '16px'
  },
  authorColumn: {
    width: '150px',
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
    .map(task => ({
      columnValues: new Map<string, GridTableColumnValue>([
        ['versions', task.parameters.find(p => p.name == 'versions')?.value],
        ['author', task.parameters.find(p => p.name == 'author')?.value],
        ['startTime', task.creationTime],
      ])} as GridTableRowParams))

  return rows?.length?<GridTable className={classes.versionsTable}
                       columns={columns} rows={rows}/>:null
}

export default ClientVersionsInProcessTable