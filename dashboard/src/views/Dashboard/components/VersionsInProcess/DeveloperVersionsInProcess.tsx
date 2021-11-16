import React, {useState} from 'react';
import clsx from 'clsx';
import { makeStyles } from '@material-ui/styles';
import {
  Card, CardContent, CardHeader,
} from '@material-ui/core';
import GridTable from "../../../../common/components/gridTable/GridTable";
import Alert from "@material-ui/lab/Alert";
import FormGroup from "@material-ui/core/FormGroup";
import {RefreshControl} from "../../../../common/components/refreshControl/RefreshControl";
import {GridTableColumnParams, GridTableColumnValue} from "../../../../common/components/gridTable/GridTableColumn";
import {useTasksQuery} from "../../../../generated/graphql";
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
  startTimeColumn: {
    width: '150px',
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

const DeveloperVersionsInProcess = () => {
  const classes = useStyles()

  const [error, setError] = useState<string>()

  const { data: tasksInProcess, refetch: getTasksInProcess } = useTasksQuery({
    variables: { type: 'BuildDeveloperVersion', onlyActive: true },
    onError(err) { setError('Query developer versions in process error ' + err.message) },
    onCompleted() { setError(undefined) }
  })

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
      name: 'startTime',
      type: 'date',
      headerName: 'Start Time',
      className: classes.startTimeColumn,
    },
    {
      name: 'comment',
      headerName: 'Comment',
      className: classes.commentColumn,
    }
  ]

  const rows = tasksInProcess?.tasks.map(task => ({
    columnValues: new Map<string, GridTableColumnValue>([
      ['service', task.parameters.find(p => p.name == 'service')?.value],
      ['version', task.parameters.find(p => p.name == 'version')?.value],
      ['author', task.parameters.find(p => p.name == 'author')?.value],
      ['comment', task.parameters.find(p => p.name == 'comment')?.value],
      ['startTime', task.creationTime]
    ])} as GridTableRowParams))

  return (
    <Card
      className={clsx(classes.root)}
    >
      <CardHeader
        action={
          <FormGroup row>
            <RefreshControl
              className={classes.control}
              refresh={() => getTasksInProcess()}
            />
          </FormGroup>
        }
        title='Developer Versions In Process'/>
      <CardContent className={classes.content}>
        <div className={classes.inner}>
          {rows?.length?
            <GridTable
             className={classes.versionsTable}
             columns={columns}
             rows={rows?rows:[]}/>:null}
          {error && <Alert className={classes.alert} severity="error">{error}</Alert>}
        </div>
      </CardContent>
    </Card>
  );
}

export default DeveloperVersionsInProcess