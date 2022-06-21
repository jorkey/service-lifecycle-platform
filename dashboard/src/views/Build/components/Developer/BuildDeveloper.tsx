import React, {useCallback, useState} from 'react';
import {makeStyles} from '@material-ui/styles';
import {Button, Card, CardContent, CardHeader,} from '@material-ui/core';
import {
  BuildStatus,
  BuildTarget,
  useBuildDeveloperServicesQuery,
  useBuildStatesQuery,
} from "../../../../generated/graphql";
import GridTable from "../../../../common/components/gridTable/GridTable";
import Alert from "@material-ui/lab/Alert";
import FormGroup from "@material-ui/core/FormGroup";
import {RefreshControl} from "../../../../common/components/refreshControl/RefreshControl";
import {useHistory, useRouteMatch} from "react-router-dom";
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
  targetsColumn: {
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

  const routeMatch = useRouteMatch()

  const { data: services, refetch: getServices } = useBuildDeveloperServicesQuery({
    fetchPolicy: 'no-cache', // base option no-cache does not work
    onError(err) { setError('Query developer services error ' + err.message) },
    onCompleted() { setError(undefined) }
  })
  const { data: buildStates, refetch: getBuildStates } = useBuildStatesQuery({
    variables: { targets: [ BuildTarget.DeveloperVersion ] },
    fetchPolicy: 'no-cache', // base option no-cache does not work
    onError(err) { setError('Query build states error ' + err.message) },
    onCompleted() { setError(undefined) }
  })

  const handleOnClick = useCallback((service: string) => {
    const task = buildStates?.buildStates
      .find(build => build.service == service && build.status == BuildStatus.InProcess)?.task
    return task ?
      history.push(`${routeMatch.url}/monitor/${task}`) :
      history.push(`${routeMatch.url}/start/${service}`)
  }, [ buildStates, history ]);

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
      name: 'targets',
      headerName: 'Targets',
      className: classes.targetsColumn,
    },
    {
      name: 'status',
      headerName: 'Status',
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
      const buildState = buildStates?.buildStates.find(build => build.service == service)
      return new Map<string, GridTableCellParams>([
          ['service', { value: service }],
          ['version', { value: buildState?buildState.version:'' }],
          ['author', { value: buildState?buildState.author:'' }],
          ['time', { value: buildState?buildState.time:'' }],
          ['comment', { value: buildState?buildState.comment:'' }],
          ['targets', { value: buildState?buildState.targets.toString():'' }],
          ['status', { value: buildState?buildState.status.toString():'' }],
          ['actions', { value: [<Button key='0' title='Start Build' onClick={ () => handleOnClick(service) }>
            {buildState?.status == BuildStatus.InProcess?<VisibilityIcon/>:<BuildIcon/>}
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
              refresh={ () => { getServices(); getBuildStates() }}
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