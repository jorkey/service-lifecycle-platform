import React, {useState} from 'react';
import clsx from 'clsx';
import { makeStyles } from '@material-ui/styles';
import {
  Card,
  CardContent, CardHeader,
} from '@material-ui/core';
import {
  useClientVersionsInfoQuery,
  useDeveloperVersionsInfoQuery,
} from "../../../../generated/graphql";
import GridTable from "../../../../common/components/gridTable/GridTable";
import {Version} from "../../../../common";
import Alert from "@material-ui/lab/Alert";
import FormGroup from "@material-ui/core/FormGroup";
import {RefreshControl} from "../../../../common/components/refreshControl/RefreshControl";
import {GridTableColumnParams, GridTableColumnValue} from "../../../../common/components/gridTable/GridTableColumn";

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
  installedByColumn: {
    width: '150px',
    padding: '4px',
  },
  installTimeColumn: {
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

const LastClientVersions = () => {
  const classes = useStyles()

  const [error, setError] = useState<string>()

  const {data:clientVersionsInfo, refetch:getClientVersionsInfo} = useClientVersionsInfoQuery({
    fetchPolicy: 'no-cache',
    onError(err) { setError('Query client versions error ' + err.message) },
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
    {
      name: 'installedBy',
      headerName: 'Installed By',
      className: classes.installedByColumn,
    },
    {
      name: 'installTime',
      headerName: 'Install Time',
      type: 'date',
      className: classes.installTimeColumn,
    },
  ]

  const rows = clientVersionsInfo?.clientVersionsInfo
    .sort((v1, v2) =>
      Version.compareClientDistributionVersions(v2.version, v1.version))
    .map(
        version => new Map<string, GridTableColumnValue>([
      ['service', version.service],
      ['version', Version.clientDistributionVersionToString(version.version)],
      ['author', version.buildInfo.author],
      ['comment', version.buildInfo.comment?version.buildInfo.comment:''],
      ['creationTime', version.buildInfo.time],
      ['installedBy', version.installInfo.account],
      ['installTime', version.installInfo.time],
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
              refresh={() => getClientVersionsInfo()}
            />
          </FormGroup>
        }
        title='Last Client Versions'
      />
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

export default LastClientVersions