import React, {useEffect, useState} from "react";
import {LogLine, useSubscribeTaskLogsSubscription} from "../../../generated/graphql";
import GridTable from "../gridTable/GridTable";
import {GridTableColumnParams, GridTableColumnValue} from "../gridTable/GridTableColumn";
import {makeStyles} from "@material-ui/core/styles";

const useStyles = makeStyles(theme => ({
  logsTable: {
  },
  dateColumn: {
    width: '200px',
    padding: '4px',
    paddingLeft: '16px'
  },
  levelColumn: {
    width: '200px',
    padding: '4px',
    paddingLeft: '16px'
  },
  unitColumn: {
    width: '200px',
    padding: '4px',
    paddingLeft: '16px'
  },
  messageColumn: {
    padding: '4px',
    paddingLeft: '16px'
  },
}))

interface LogsTableParams {
  lines: LogLine[]
}

export const LogsTable = (props: LogsTableParams) => {
  const { lines } = props

  const classes = useStyles();

  const columns: GridTableColumnParams[] = [
    {
      name: 'date',
      headerName: 'Date',
      className: classes.dateColumn,
      type: 'date',
    },
    {
      name: 'level',
      headerName: 'Level',
      className: classes.levelColumn
    },
    {
      name: 'unit',
      headerName: 'Unit',
      className: classes.unitColumn
    },
    {
      name: 'message',
      headerName: 'Line',
      className: classes.messageColumn
    },
  ]

  const rows = lines.map(line => new Map<string, GridTableColumnValue>([
    ['date', line.date],
    ['level', line.level],
    ['unit', line.unit],
    ['message', line.message]
  ]))

  return <GridTable
    className={classes.logsTable}
    columns={columns}
    rows={rows}
  />;
}
