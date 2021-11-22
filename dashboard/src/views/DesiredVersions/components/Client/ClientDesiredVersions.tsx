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
import {GridTableColumnParams, GridTableColumnValue} from "../../../../common/components/gridTable/GridTableColumn";
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
    width: '150px',
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
  },
  historyButton: {
    width: '100px',
    textTransform: 'none',
  },
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
    variables: { limit: 25 },
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
      name: 'buildTime',
      headerName: 'Build Time',
      type: 'date',
      className: classes.timeColumn,
    },
    {
      name: 'comment',
      headerName: 'Comment',
      className: classes.commentColumn,
    },
    {
      name: 'installTime',
      headerName: 'Install Time',
      type: 'date',
      className: classes.timeColumn,
    }
  ]

  const versions = time?desiredVersionsHistory?.clientDesiredVersionsHistory?.find(v => v.time == time)?.versions:
    desiredVersions?.clientDesiredVersions

  const rows = versions?.map(version => {
    const info = clientVersions?.clientVersionsInfo?.find(v => v.service == version.service)
    const author = info?.buildInfo.author
    const buildTime = info?.buildInfo.time
    const comment = info?.buildInfo.comment
    const installTime = info?.installInfo.time
    return {
      columnValues: new Map<string, GridTableColumnValue>([
        ['service', version.service],
        ['version', Version.clientDistributionVersionToString(version.version)],
        ['author', author],
        ['buildTime', buildTime],
        ['comment', comment],
        ['installTime', installTime]
      ])
    }}
  )

  return (
    <Card
      className={clsx(classes.root)}
    >
      <CardHeader
        action={
          <FormGroup row>
            <Button
              className={classes.historyButton}
              color="primary"
              variant="contained"
              // onClick={() => tableRef.current?.toTop()}
            >
              History
            </Button>
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
