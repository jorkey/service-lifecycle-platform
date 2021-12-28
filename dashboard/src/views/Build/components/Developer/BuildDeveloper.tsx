import React, {useCallback, useState} from 'react';
import clsx from 'clsx';
import { makeStyles } from '@material-ui/styles';
import {
  Button,
  Card,
  CardContent, CardHeader,
} from '@material-ui/core';
import {
  useDeveloperServicesQuery,
  useDeveloperVersionsInfoQuery, useTasksQuery
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
    textTransform: 'none'
  },
  alert: {
    marginTop: 25
  }
}));

const BuildDeveloper = () => {
  const classes = useStyles()

  const history = useHistory()
  const [error, setError] = useState<string>()

  const { data: services, refetch: getServices } = useDeveloperServicesQuery({
    fetchPolicy: 'no-cache', // base option no-cache does not work
    onError(err) { setError('Query developer services error ' + err.message) },
    onCompleted() { setError(undefined) }
  })
  const { data: completedVersions, refetch: getCompletedVersions } = useDeveloperVersionsInfoQuery({
    fetchPolicy: 'no-cache', // base option no-cache does not work
    onError(err) { setError('Query developer versions error ' + err.message) },
    onCompleted() { setError(undefined) }
  })
  const { data: tasksInProcess, refetch: getTasksInProcess } = useTasksQuery({
    fetchPolicy: 'no-cache', // base option no-cache does not work
    variables: { type: 'BuildDeveloperVersion', onlyActive: true },
    onError(err) { setError('Query developer versions in process error ' + err.message) },
    onCompleted() { setError(undefined) }
  })

  const handleOnClick = useCallback((service: string) => {
    tasksInProcess?.tasks.find(task => task.parameters.find(p => {
        return p.name == 'service' && p.value == service })) ?
      history.push('developer/monitor/' + service) :
      history.push('developer/start/' + service)
  }, [ tasksInProcess, history ]);

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
      type: 'date',
      className: classes.timeColumn,
    },
    {
      name: 'comment',
      headerName: 'Comment',
      className: classes.commentColumn,
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

  const rows = services?.developerServicesConfig.map(s => s.service).map(
    service => {
      const versionInProcess = tasksInProcess?.tasks.find(task => task.parameters.find(p => {
        return p.name == 'service' && p.value == service }))
      const completedVersion = completedVersions?.developerVersionsInfo?.filter(version => version.service == service)
        .sort((v1, v2) => Version.compareBuilds(v2.version.build, v1.version.build))[0]
      const version = versionInProcess?versionInProcess.parameters.find(p => p.name == 'version')?.value:
        completedVersion?Version.buildToString(completedVersion.version.build):undefined
      const author = versionInProcess?versionInProcess.parameters.find(p => p.name == 'author')?.value:
        completedVersion?completedVersion.buildInfo.author:undefined
      const time = versionInProcess?versionInProcess.creationTime:
        completedVersion?completedVersion.buildInfo.time:undefined
      const comment = versionInProcess?versionInProcess.parameters.find(p => p.name == 'comment')?.value:
        completedVersion?completedVersion.buildInfo.comment:undefined
      const status = versionInProcess?'In Process':
        completedVersion?'Completed':undefined
      return new Map<string, GridTableCellParams>([
          ['service', { value: service }],
          ['version', { value: version }],
          ['author', { value: author }],
          ['time', { value: time }],
          ['comment', { value: comment }],
          ['status', { value: status }],
          ['actions', { value: [<Button key='0' onClick={ () => handleOnClick(service) }>
            {versionInProcess?<VisibilityIcon/>:<BuildIcon/>}
          </Button>] }]
        ])
    })

  return (
    <Card
      className={clsx(classes.root)}
    >
      <CardHeader
        action={
          <FormGroup row>
            <RefreshControl
              className={classes.control}
              refresh={ () => { getServices(); getTasksInProcess(); getCompletedVersions() }}
            />
          </FormGroup>
        }
        title='Build Developer Service Version'
      />
      <CardContent className={classes.content}>
        <div className={classes.inner}>
          <GridTable
           className={classes.versionsTable}
           columns={columns}
           rows={rows?rows:[]}
           onClick={(row) => { if (rows) handleOnClick(rows[row].get('service')!.value! as string)}}
          />
          {error && <Alert className={classes.alert} severity="error">{error}</Alert>}
        </div>
      </CardContent>
    </Card>
  );
}

export default BuildDeveloper