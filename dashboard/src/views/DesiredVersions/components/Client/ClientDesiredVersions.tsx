import React, {useEffect, useState} from 'react';

import {NavLink as RouterLink, RouteComponentProps, useHistory} from "react-router-dom"

import { makeStyles } from '@material-ui/core/styles';
import Alert from "@material-ui/lab/Alert";
import {
  ClientDesiredVersion,
  useClientDesiredVersionsHistoryQuery,
  useClientDesiredVersionsQuery, useClientVersionsInfoQuery,
  useDeveloperServicesQuery, useDeveloperVersionsInfoQuery, useSetClientDesiredVersionsMutation,
  useTasksQuery
} from "../../../../generated/graphql";
import {GridTableColumnParams, GridTableCellParams} from "../../../../common/components/gridTable/GridTableColumn";
import {Box, Button, Card, CardContent, CardHeader, FormControlLabel} from "@material-ui/core";
import VisibilityIcon from "@material-ui/icons/Visibility";
import BuildIcon from "@material-ui/icons/Build";
import {Version} from "../../../../common";
import clsx from "clsx";
import FormGroup from "@material-ui/core/FormGroup";
import {RefreshControl} from "../../../../common/components/refreshControl/RefreshControl";
import GridTable from "../../../../common/components/gridTable/GridTable";
import TimeSelector from "../../../../common/components/timeSelector/TimeSelector";
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
    paddingLeft: '16px'
  },
  boldVersionColumn: {
    width: '150px',
    padding: '4px',
    paddingLeft: '16px',
    fontWeight: 600
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
  controls: {
    marginTop: 25,
    display: 'flex',
    justifyContent: 'flex-end',
    p: 2
  },
  control: {
    marginLeft: '10px',
    marginRight: '10px',
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
  const [timeSelector, setTimeSelector] = useState(false)
  const [desiredVersions, setDesiredVersions] = useState<ClientDesiredVersion[] | undefined>(undefined)
  const [error, setError] = useState<string>()

  const { data: originalDesiredVersions, refetch: getOriginalDesiredVersions } = useClientDesiredVersionsQuery({
    onCompleted(versions) {
      setDesiredVersions(versions.clientDesiredVersions)
    },
    onError(err) { setError('Query desired versions error ' + err.message) }
  })
  const { data: desiredVersionsHistory, refetch: getDesiredVersionsHistory } = useClientDesiredVersionsHistoryQuery({
    variables: { limit: 25 },
    onCompleted(versions) {
      if (versions.clientDesiredVersionsHistory.length) {
        setTime(versions.clientDesiredVersionsHistory[versions.clientDesiredVersionsHistory.length-1].time)
      }
    },
    onError(err) { setError('Query desired versions history error ' + err.message) },
  })
  const [ changeDesiredVersions ] = useSetClientDesiredVersionsMutation()
  const { data: clientVersions, refetch: getClientVersions } = useClientVersionsInfoQuery({
    onError(err) { setError('Query client versions error ' + err.message) },
  })

  useEffect(() => { if (!timeSelector)  {
    getOriginalDesiredVersions()
    getDesiredVersionsHistory()
  } },[ timeSelector ])

  console.log('render')

  const columns: Array<GridTableColumnParams> = [
    {
      name: 'service',
      headerName: 'Service',
      className: classes.serviceColumn,
    },
    {
      name: 'version',
      type: 'select',
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

  const rows = desiredVersions?.map(v => v.service).map(service => {
    const version = desiredVersions?.find(v => v.service == service)?.version!
    const info = clientVersions?.clientVersionsInfo?.find(v => v.service == service)
    const author = info?.buildInfo.author
    const buildTime = info?.buildInfo.time
    const comment = info?.buildInfo.comment
    const installTime = info?.installInfo.time
    const originalVersion = originalDesiredVersions?.clientDesiredVersions.find(v => v.service == service)?.version
    const modified = Version.compareClientDistributionVersions(version, originalVersion)
    return new Map<string, GridTableCellParams>([
        ['service', { value: service }],
        ['version', {
          value: Version.clientDistributionVersionToString(version),
          className: modified?classes.boldVersionColumn:undefined
        }],
        ['author', { value: author }],
        ['buildTime', { value: buildTime }],
        ['comment', { value: comment }],
        ['installTime', { value: installTime }]
      ])})

  return rows && desiredVersionsHistory?.clientDesiredVersionsHistory?(
    <Card
      className={clsx(classes.root)}
    >
      <CardHeader
        action={
          <FormGroup row>
            <FormControlLabel
              label={null}
              control={
                !timeSelector ?
                  <TimeSelector time={time}
                                times={desiredVersionsHistory.clientDesiredVersionsHistory.map(v => v.time)}
                                onSelected={(t) => {
                                  console.log('on selected time ' + t)
                                  setTime(t)
                                  setDesiredVersions(desiredVersionsHistory?.clientDesiredVersionsHistory?.find(v => v.time == time)?.versions)
                                }}
                  /> :
                  <Button
                    className={classes.historyButton}
                    color="primary"
                    variant="contained"
                    onClick={() => setTimeSelector(true)}
                  >
                    History
                  </Button>
              }/>
            <RefreshControl className={classes.control}
                            refresh={() => {
                              setTimeSelector(false)
                              getOriginalDesiredVersions(); getDesiredVersionsHistory(); getClientVersions()
                            }}
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
          {timeSelector?<Box className={classes.controls}>
            <Button className={classes.control}
                    color="primary"
                    variant="contained"
                    onClick={() => setTimeSelector(false)}
            >
              Cancel
            </Button>
            <Button className={classes.control}
                    color="primary"
                    variant="contained"
                    // disabled={!validate()}
                    onClick={() => setTimeSelector(false)}
            >
              Save
            </Button>
          </Box>:null}
        </div>
      </CardContent>
    </Card>
  ) : null
}

export default ClientDesiredVersions;
