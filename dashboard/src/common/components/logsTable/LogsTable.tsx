import React, {useEffect, useState} from "react";
import {LogLine, useSubscribeTaskLogsSubscription} from "../../../generated/graphql";
import GridTable from "../gridTable/GridTable";
import {GridTableColumnParams, GridTableColumnValue} from "../gridTable/GridTableColumn";
import {makeStyles} from "@material-ui/core/styles";

const useStyles = makeStyles(theme => ({
  div: {
    display: 'relative'
  },
  logsTable: {
    height: 'calc(100vh - 550px)',
  },
  timeColumn: {
    width: '200px',
    padding: '4px',
    paddingLeft: '16px'
  },
  levelColumn: {
    width: '100px',
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
      name: 'time',
      headerName: 'Time',
      className: classes.timeColumn,
      type: 'date',
    },
    {
      name: 'level',
      headerName: 'Level',
      className: classes.levelColumn
    },
    {
      name: 'message',
      headerName: 'Line',
      className: classes.messageColumn
    },
  ]

  const rows = lines.map(line => new Map<string, GridTableColumnValue>([
    ['time', line.time],
    ['level', line.level],
    ['unit', line.unit],
    ['message', line.message]
  ]))

  console.log('rows ' + rows.length)

  return <GridTable
    className={classes.logsTable}
    columns={columns}
    rows={rows}
  />;
}
