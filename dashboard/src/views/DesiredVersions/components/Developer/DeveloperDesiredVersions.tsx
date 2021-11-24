import React, {useState} from 'react';

import {RouteComponentProps, useHistory} from "react-router-dom"

import { makeStyles } from '@material-ui/core/styles';
import Alert from "@material-ui/lab/Alert";
import {
  useClientDesiredVersionsHistoryQuery,
  useClientDesiredVersionsQuery, useClientVersionsInfoQuery,
  useDeveloperServicesQuery, useDeveloperVersionsInfoQuery, useSetClientDesiredVersionsMutation,
  useTasksQuery
} from "../../../../generated/graphql";
import {GridTableColumnParams, GridTableCellParams} from "../../../../common/components/gridTable/GridTableColumn";
import {Button, Card, CardContent, CardHeader} from "@material-ui/core";
import VisibilityIcon from "@material-ui/icons/Visibility";
import BuildIcon from "@material-ui/icons/Build";
import {Version} from "../../../../common";
import clsx from "clsx";
import FormGroup from "@material-ui/core/FormGroup";
import {RefreshControl} from "../../../../common/components/refreshControl/RefreshControl";
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
  serviceColumn: {
    width: '150px',
    padding: '4px',
    paddingLeft: '16px'
  },
  versionColumn: {
    width: '100px',
    padding: '4px',
    paddingLeft: '16px'
  },
  authorColumn: {
    width: '150px',
    padding: '4px',
    paddingLeft: '16px'
  },
  commentColumn: {
    padding: '4px',
    paddingLeft: '16px'
  },
  timeColumn: {
    width: '200px',
    padding: '4px',
    paddingLeft: '16px'
  },
  actionsColumn: {
    width: '120px',
    padding: '4px',
    paddingRight: '30px',
    textAlign: 'right'
  },
  control: {
    paddingLeft: '10px',
    textTransform: 'none'
  },
  alert: {
    marginTop: 25
  }
}));

interface ClientDesiredVersionsRouteParams {
}

interface ClientDesiredVersionsParams extends RouteComponentProps<ClientDesiredVersionsRouteParams> {
  fromUrl: string
}

const ClientDesiredVersions = (props: ClientDesiredVersionsParams) => {
  const classes = useStyles()

  const [time, setTime] = useState<Date>()
  const [error, setError] = useState<string>()

  const { data: desiredVersions, refetch: getDesiredVersions } = useClientDesiredVersionsQuery({
    onError(err) { setError('Query desired versions error ' + err.message) },
  })
  const { data: desiredVersionsHistory, refetch: getDesiredVersionsHistory } = useClientDesiredVersionsHistoryQuery({
    onError(err) { setError('Query desired versions history error ' + err.message) },
  })
  const [ setDesiredVersions ] = useSetClientDesiredVersionsMutation()
  const { data: clientVersions, refetch: getClientVersions } = useClientVersionsInfoQuery({
    onError(err) { setError('Query client versions error ' + err.message) },
  })

  const columns: Array<GridTableColumnParams> = [
    {
      name: 'service',
      headerName: 'Service',
      className: classes.serviceColumn,
    },
    {
      name: 'version',
      headerName: 'Desired Version',
      className: classes.versionColumn,
    },
    {
      name: 'author',
      headerName: 'Author',
      className: classes.authorColumn,
    },
    {
      name: 'time',
      headerName: 'Time',
      type: 'date',
      className: classes.timeColumn,
    },
    {
      name: 'comment',
      headerName: 'Comment',
      className: classes.commentColumn,
    }
  ]

  const versions = time?desiredVersionsHistory?.clientDesiredVersionsHistory?.find(v => v.time == time)?.versions:
    desiredVersions?.clientDesiredVersions

  const rows = versions?.map(version => {
    const author = clientVersions?.clientVersionsInfo?.find(v => v.service)?.buildInfo.author
    const time = clientVersions?.clientVersionsInfo?.find(v => v.service)?.buildInfo.time
    const comment = clientVersions?.clientVersionsInfo?.find(v => v.service)?.buildInfo.comment
    return new Map<string, GridTableCellParams>([
      ['service', { value: version.service }],
      ['version', { value: Version.clientDistributionVersionToString(version.version) }],
      ['author', { value: author }],
      ['time', { value: time }],
      ['comment', { value: comment }]
      ])})

  return (
    <Card
      className={clsx(classes.root)}
    >
      <CardHeader
        action={
          <FormGroup row>
            <RefreshControl
              className={classes.control}
              refresh={ () => { getDesiredVersions(); getDesiredVersionsHistory(); getClientVersions() }}
            />
          </FormGroup>
        }
        title='Client Desired Versions'
      />
      <CardContent className={classes.content}>
        <div className={classes.inner}>
          <GridTable
            className={classes.versionsTable}
            columns={columns}
            rows={rows?rows:[]}
          />
          {error && <Alert className={classes.alert} severity="error">{error}</Alert>}
        </div>
      </CardContent>
    </Card>
  );
}

export default ClientDesiredVersions;
