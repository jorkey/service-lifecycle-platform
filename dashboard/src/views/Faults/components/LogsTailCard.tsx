import React from "react";
import {
  LogLine,
} from "../../../generated/graphql";
import {makeStyles} from "@material-ui/core/styles";
import GridTable from "../../../common/components/gridTable/GridTable";
import {Card, CardContent, CardHeader} from "@material-ui/core";
import {GridTableCellParams} from "../../../common/components/gridTable/GridTableCell";
import {GridTableColumnParams} from "../../../common/components/gridTable/GridTableColumn";

const useStyles = makeStyles(theme => ({
  content: {
    padding: 0
  },
  inner: {
    minWidth: 800
  },
  logsTable: {
    height: '250px'
  },
  timeColumn: {
    width: '200px',
    minWidth: '200px',
  },
  levelColumn: {
    width: '100px',
    minWidth: '100px',
  },
  messageColumn: {
    whiteSpace: 'pre',
  },
}))

interface LogsTailCardParams {
  lines: LogLine[]
}

export const LogsTailCard = (props: LogsTailCardParams) => {
  const { lines } = props

  const classes = useStyles()

  const columns: GridTableColumnParams[] = [
    {
      name: 'time',
      headerName: 'Time',
      className: classes.timeColumn,
      type: 'time',
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

  const rows = lines
    .map(line => (
      new Map<string, GridTableCellParams>([
        ['time', { value: line.time }],
        ['level', { value: line.level }],
        ['unit', { value: line.unit }],
        ['message', { value: line.message }]
      ])))

  return <Card>
    <CardHeader title={'Logs Tail'}/>
    <CardContent className={classes.content}>
      <div className={classes.inner}>
        <GridTable
          className={classes.logsTable}
          columns={columns}
          rows={rows}
          scrollToRow={rows.length-1}
        />
      </div>
    </CardContent>
  </Card>
}