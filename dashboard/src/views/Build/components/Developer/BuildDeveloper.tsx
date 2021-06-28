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
  useDeveloperVersionsInfoQuery, useDeveloperVersionsInProcessQuery
} from "../../../../generated/graphql";
import GridTable from "../../../../common/components/gridTable/GridTable";
import {Version} from "../../../../common";
import Alert from "@material-ui/lab/Alert";
import FormGroup from "@material-ui/core/FormGroup";
import {RefreshControl} from "../../../../common/components/refreshControl/RefreshControl";
import {useHistory} from "react-router-dom";
import BuildIcon from "@material-ui/icons/Build";
import VisibilityIcon from "@material-ui/icons/Visibility";
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
  statusColumn: {
    width: '100px',
    padding: '4px',
    paddingLeft: '16px'
  },
  timeColumn: {
    width: '200px',
    padding: '4px',
    paddingLeft: '16px'
  },
  actionsColumn: {
    width: '200px',
    padding: '4px',
    paddingRight: '40px',
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

const BuildDeveloper = () => {
  const classes = useStyles()

  const [error, setError] = useState<string>()
  const history = useHistory()

  const { data: services, refetch: getServices } = useDeveloperServicesQuery({
    fetchPolicy: 'no-cache',
    onError(err) { setError('Query developer services error ' + err.message) },
    onCompleted() { setError(undefined) }
  })
  const { data: completedVersions, refetch: getCompletedVersions } = useDeveloperVersionsInfoQuery({
    fetchPolicy: 'no-cache',
    onError(err) { setError('Query developer versions error ' + err.message) },
    onCompleted() { setError(undefined) }
  })
  const { data: versionsInProcess, refetch: getVersionsInProcess } = useDeveloperVersionsInProcessQuery({
    fetchPolicy: 'no-cache',
    onError(err) { setError('Query developer versions in process error ' + err.message) },
    onCompleted() { setError(undefined) }
  })

  const handleOnClick = useCallback((service: string) => {
    versionsInProcess?.developerVersionsInProcess.find(version => version.service == service) ?
      history.push('developer/monitor/' + service) :
      history.push('developer/start/' + service)
  }, [ versionsInProcess, history ]);

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

  const rows = services?.developerServices.map(
    service => {
      const versionInProcess = versionsInProcess?.developerVersionsInProcess.find(version => version.service == service)
      const completedVersion = completedVersions?.developerVersionsInfo?.find(version => version.service == service)
      const version = versionInProcess?Version.buildToString(versionInProcess.version.build):
        completedVersion?Version.buildToString(completedVersion.version.build):undefined
      const author = versionInProcess?versionInProcess.author:
        completedVersion?completedVersion.buildInfo.author:undefined
      const time = versionInProcess?versionInProcess.startTime:
        completedVersion?completedVersion.buildInfo.date:undefined
      const comment = versionInProcess?versionInProcess.comment:
        completedVersion?completedVersion.buildInfo.comment:undefined
      const status = versionInProcess?'In Process':
        completedVersion?'Completed':undefined
      return new Map<string, GridTableColumnValue>([
        ['service', service],
        ['version', version],
        ['author', author],
        ['time', time],
        ['comment', comment],
        ['status', status],
        ['actions', [<Button key='0' onClick={ () => handleOnClick(service) }>
          {versionInProcess?<VisibilityIcon/>:<BuildIcon/>}
        </Button>]]
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
              refresh={ () => { getServices(); getVersionsInProcess(); getCompletedVersions() }}
            />
          </FormGroup>
        }
        title='Build service'
      />
      <CardContent className={classes.content}>
        <div className={classes.inner}>
          <GridTable
           className={classes.versionsTable}
           columns={columns}
           rows={rows?rows:[]}
           onClick={(row, values) => handleOnClick(values.get('service')! as string)}
          />
          {error && <Alert className={classes.alert} severity="error">{error}</Alert>}
        </div>
      </CardContent>
    </Card>
  );
}

export default BuildDeveloper