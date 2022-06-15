import React, {useCallback, useState} from 'react';
import { makeStyles } from '@material-ui/styles';
import {
  Button,
  Card,
  CardContent, CardHeader,
} from '@material-ui/core';
import {
  BuildStatus,
  useBuildDeveloperServicesQuery, useClientBuildsQuery, useDeveloperBuildsQuery,
} from "../../../../generated/graphql";
import GridTable from "../../../../common/components/gridTable/GridTable";
import {Version} from "../../../../common";
import Alert from "@material-ui/lab/Alert";
import FormGroup from "@material-ui/core/FormGroup";
import {RefreshControl} from "../../../../common/components/refreshControl/RefreshControl";
import {useHistory} from "react-router-dom";
import BuildIcon from "@material-ui/icons/Build";
import VisibilityIcon from "@material-ui/icons/Visibility";
import {GridTableColumnParams} from "../../../../common/components/gridTable/GridTableColumn";
import {GridTableCellParams} from "../../../../common/components/gridTable/GridTableCell";

const useStyles = makeStyles((theme:any) => ({
  inner: {
    minWidth: 800
  },
  versionsTable: {
  },
  serviceColumn: {
    width: '150px',
  },
  versionColumn: {
    width: '150px',
  },
  authorColumn: {
    width: '150px',
  },
  commentColumn: {
  },
  statusColumn: {
    width: '100px',
  },
  timeColumn: {
    width: '200px',
  },
  actionsColumn: {
    width: '120px',
    paddingRight: '30px',
    textAlign: 'right'
  },
  control: {
  },
  alert: {
    marginTop: 25
  }
}));

const BuildDeveloper = () => {
  const classes = useStyles()

  const history = useHistory()
  const [error, setError] = useState<string>()

  const { data: services, refetch: getServices } = useBuildDeveloperServicesQuery({
    fetchPolicy: 'no-cache', // base option no-cache does not work
    onError(err) { setError('Query developer services error ' + err.message) },
    onCompleted() { setError(undefined) }
  })
  const { data: developerBuilds, refetch: getDeveloperBuilds } = useDeveloperBuildsQuery({
    fetchPolicy: 'no-cache', // base option no-cache does not work
    onError(err) { setError('Query developer builds error ' + err.message) },
    onCompleted() { setError(undefined) }
  })
  const { data: clientBuilds, refetch: getClientDeveloperBuilds } = useClientBuildsQuery({
    fetchPolicy: 'no-cache', // base option no-cache does not work
    onError(err) { setError('Query client builds error ' + err.message) },
    onCompleted() { setError(undefined) }
  })

  const handleOnClick = useCallback((service: string) => {
    const task = developerBuilds?.developerBuilds.find(build => build.service == service)?.task
    return task ? history.push('developer/monitor/' + task) :
      history.push('developer/start/' + service)
  }, [ developerBuilds, history ]);

  const columns: Array<GridTableColumnParams> = [
    {
      name: 'service',
      headerName: 'Service',
      className: classes.serviceColumn,
    },
    {
      name: 'version',
      headerName: 'Last Version',
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
      type: 'relativeTime',
      className: classes.timeColumn,
    },
    {
      name: 'comment',
      headerName: 'Comment',
      className: classes.commentColumn,
    },
    {
      name: 'developerStatus',
      headerName: 'Developer Status',
      className: classes.statusColumn,
    },
    {
      name: 'clientStatus',
      headerName: 'Client Status',
      className: classes.statusColumn,
    },
    {
      name: 'actions',
      headerName: 'Actions',
      type: 'elements',
      className: classes.actionsColumn
    }
  ]

  const rows = services?.buildDeveloperServicesConfig.map(s => s.service).sort().map(
    service => {
      const developerState = developerBuilds?.developerBuilds.find(build => build.service == service)
      const clientState = clientBuilds?.clientBuilds.find(build => build.service == service)
      return new Map<string, GridTableCellParams>([
          ['service', { value: developerState?developerState.service:'' }],
          ['version', { value: developerState?Version.buildToString(developerState.version.build):'' }],
          ['author', { value: developerState?developerState.author:'' }],
          ['time', { value: developerState?developerState.time:'' }],
          ['comment', { value: developerState?developerState.comment:'' }],
          ['developerStatus', { value: developerState?developerState.status.toString():'' }],
          ['clientStatus', { value: clientState?clientState.status.toString():'' }],
          ['actions', { value: [<Button key='0' title='Start Build' onClick={ () => handleOnClick(service) }>
            {developerState?.status == BuildStatus.InProcess?<VisibilityIcon/>:<BuildIcon/>}
          </Button>] }]
        ])
    })

  return (
    <Card>
      <CardHeader
        action={
          <FormGroup row>
            <RefreshControl
              className={classes.control}
              refresh={ () => { getServices(); getDeveloperBuilds() }}
            />
          </FormGroup>
        }
        title='Build Developer Service Version'
      />
      <CardContent>
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

export default BuildDeveloper