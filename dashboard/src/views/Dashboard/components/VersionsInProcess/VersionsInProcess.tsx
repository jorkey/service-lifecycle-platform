import React, {useState} from 'react';
import clsx from 'clsx';
import { makeStyles } from '@material-ui/styles';
import {
  Card, CardContent, CardHeader,
} from '@material-ui/core';
import {
  useDeveloperVersionsInProcessLazyQuery, useDeveloperVersionsInProcessQuery,
} from "../../../../generated/graphql";
import GridTable from "../../../../common/components/gridTable/GridTable";
import {Version} from "../../../../common";
import {GridTableColumnParams, GridTableColumnValue} from "../../../../common/components/gridTable/GridTableRow";
import Alert from "@material-ui/lab/Alert";
import FormGroup from "@material-ui/core/FormGroup";
import {RefreshControl} from "../../../../common/components/refreshControl/RefreshControl";

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
  startTimeColumn: {
    width: '200px',
    padding: '4px',
    paddingLeft: '16px'
  },
  taskColumn: {
    width: '100px',
    padding: '4px',
    paddingLeft: '16px'
  },
  control: {
    paddingLeft: '10px',
    textTransform: 'none'
  },
  alert: {
    marginTop: 25
  },
}));

const VersionsInProcess = () => {
  const classes = useStyles()

  const [error, setError] = useState<string>()

  const { data: versionsInProcess, refetch: getVersionsInProcess } = useDeveloperVersionsInProcessQuery({
    fetchPolicy: 'no-cache',
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
      headerName: 'Start Time',
      className: classes.startTimeColumn,
    },
    {
      name: 'comment',
      headerName: 'Comment',
      className: classes.commentColumn,
    }
  ]

  const rows = versionsInProcess?.developerVersionsInProcess.map(
      version => new Map<string, GridTableColumnValue>([
    ['service', version.service],
    ['version', Version.developerVersionToString(version.version)],
    ['author', version.author],
    ['comment', version.comment?version.comment:''],
    ['startTime', version.startTime]
  ]))

  return (
    <Card
      className={clsx(classes.root)}
    >
      <CardHeader
        action={
          <FormGroup row>
            <RefreshControl
              className={classes.control}
              refresh={() => getVersionsInProcess()}
            />
          </FormGroup>
        }
        title='Versions In Process'/>
      <CardContent className={classes.content}>
        <div className={classes.inner}>
          <GridTable
           className={classes.versionsTable}
           columns={columns}
           rows={rows?rows:[]}/>
          {error && <Alert className={classes.alert} severity="error">{error}</Alert>}
        </div>
      </CardContent>
    </Card>
  );
}

export default VersionsInProcess