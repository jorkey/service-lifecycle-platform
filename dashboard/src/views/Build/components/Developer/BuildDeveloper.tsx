import React, {useCallback, useState} from 'react';
import {makeStyles} from '@material-ui/styles';
import {Button, Card, CardContent, CardHeader, InputLabel, Link,} from '@material-ui/core';
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
    width: '125px',
  },
  clientVersionColumn: {
    width: '150px',
  },
  authorColumn: {
    width: '150px',
  },
  commentColumn: {
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
  },
  successText: {
    color: "green",
    paddingBottom: '4px'
  },
  failureText: {
    color: "red",
    textDecoration: "line-through",
    paddingBottom: '4px'
  },
  inProcessText: {
    color: "blue",
    paddingBottom: '4px'
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
  const { data: developerBuildStates, refetch: getDeveloperBuildStates } = useBuildStatesQuery({
    variables: { target: BuildTarget.DeveloperVersion },
    fetchPolicy: 'no-cache', // base option no-cache does not work
    onError(err) { setError('Query developer build states error ' + err.message) },
    onCompleted() { setError(undefined) }
  })
  const { data: clientBuildStates, refetch: getClientBuildStates } = useBuildStatesQuery({
    variables: { target: BuildTarget.ClientVersion },
    fetchPolicy: 'no-cache', // base option no-cache does not work
    onError(err) { setError('Query client build states error ' + err.message) },
    onCompleted() { setError(undefined) }
  })

  const handleOnClick = useCallback((service: string) => {
    const task = developerBuildStates?.buildStates
      .find(build => build.service == service && build.status == BuildStatus.InProcess)?.task
    return task ?
      history.push(`${routeMatch.url}/monitor/${task}`) :
      history.push(`${routeMatch.url}/start/${service}`)
  }, [ clientBuildStates, history ]);

  const columns: Array<GridTableColumnParams> = [
    {
      name: 'service',
      headerName: 'Service',
      className: classes.serviceColumn,
    },
    {
      name: 'lastBuild',
      headerName: 'Last Build',
      className: classes.versionColumn,
    },
    {
      name: 'lastClientBuild',
      headerName: 'Last Client Build',
      className: classes.clientVersionColumn,
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
      name: 'actions',
      headerName: 'Actions',
      type: 'elements',
      className: classes.actionsColumn
    }
  ]

  const rows = services?.buildDeveloperServicesConfig.map(s => s.service).sort().map(
    service => {
      const developerState = developerBuildStates?.buildStates.find(build => build.service == service)
      const clientState = clientBuildStates?.buildStates.find(build => build.service == service)
      return new Map<string, GridTableCellParams>([
          ['service', { value: service }],
          ['lastBuild', { value: developerState?[<Link href={'/logging/tasks/' + developerState.task} underline='always'>
              <InputLabel className={developerState.status==BuildStatus.Success?
                  classes.successText:developerState.status==BuildStatus.Failure?classes.failureText:classes.inProcessText}>
                { (developerState.status==BuildStatus.InProcess?'-> ':'') + developerState.version}
              </InputLabel>
            </Link>]:[] }],
          ['lastClientBuild', { value: clientState?[<Link href={'/logging/tasks/' + clientState.task} underline='always'>
              <InputLabel className={clientState.status==BuildStatus.Success?
                  classes.successText:clientState.status==BuildStatus.Failure?classes.failureText:classes.inProcessText}>
                { (clientState.status==BuildStatus.InProcess?'-> ':'') + clientState.version}
              </InputLabel>
            </Link>]:[] }],
          ['author', { value: developerState?developerState.author:'' }],
          ['time', { value: developerState?developerState.time:'' }],
          ['comment', { value: developerState?developerState.comment:'' }],
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
              refresh={ () => { getServices(); getDeveloperBuildStates(); getClientBuildStates() }}
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