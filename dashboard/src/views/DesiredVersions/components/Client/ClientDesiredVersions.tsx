import React, {useEffect, useState} from 'react';

import {RouteComponentProps} from "react-router-dom"
import { makeStyles } from '@material-ui/core/styles';
import {
  ClientDistributionVersion, ClientVersionInfo, TimedClientDesiredVersions,
  useClientDesiredVersionsHistoryQuery,
  useClientVersionsInfoQuery,
  useSetClientDesiredVersionsMutation,
} from "../../../../generated/graphql";
import {Version} from "../../../../common";
import {DesiredVersionsView, ServiceVersion, VersionInfo} from "../../DesiredVersionsView";

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
    padding: '8px',
    paddingLeft: '20px'
  },
  versionColumn: {
    width: '150px',
    padding: '8px',
    paddingLeft: '16px'
  },
  authorColumn: {
    width: '150px',
    padding: '8px',
    paddingLeft: '16px'
  },
  commentColumn: {
    padding: '8px',
    paddingLeft: '16px'
  },
  timeColumn: {
    width: '200px',
    padding: '8px',
    paddingLeft: '16px'
  },
  appearedAttribute: {
    color: 'green'
  },
  disappearedAttribute: {
    color: 'red'
  },
  modifiedAttribute: {
    fontWeight: 600
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
  authorText: {
    textAlign: 'left',
    paddingLeft: 25,
    paddingTop: 2
  },
  timeText: {
    textAlign: 'left',
    paddingTop: 2
  },
  timeChangeButton: {
    width: '25px',
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

  const {data: desiredVersionsHistory, refetch: getDesiredVersionsHistory} =
      useClientDesiredVersionsHistoryQuery({
    variables: {limit: 25},
    onCompleted(desiredVersionsHistory) {
      if (versionsInfo) {
        initView(desiredVersionsHistory.clientDesiredVersionsHistory, versionsInfo.clientVersionsInfo)
      }
    },
    onError(err) {
      // view.setError('Query desired versions history error ' + err.message)
    }
  })
  const {data: versionsInfo, refetch: getVersionsInfo} = useClientVersionsInfoQuery({
    onCompleted(versionsInfo) {
      if (desiredVersionsHistory) {
        initView(desiredVersionsHistory.clientDesiredVersionsHistory, versionsInfo.clientVersionsInfo)
      }
    },
    onError(err) {
      // view.setError('Query client versions error ' + err.message)
    },
  })

  const [changeDesiredVersions] = useSetClientDesiredVersionsMutation()

  const [ view, setView ] = useState<DesiredVersionsView<ClientDistributionVersion>>()
  const [ version, setTimestamp ] = useState(new Date())

  const initView = (desiredVersionsHistory: TimedClientDesiredVersions[], versionsInfo: ClientVersionInfo[]) => {
    const view = new DesiredVersionsView<ClientDistributionVersion>(
      'Client Desired Versions',
      desiredVersionsHistory,
      versionsInfo.map(v => { return { version: v,
        info: { author: v.buildInfo.author, buildTime: v.buildInfo.time.toLocaleString(), comment: v.buildInfo.comment }}}),
      (v1, v2) =>
        Version.compareClientDistributionVersions(v1, v2),
      (v) =>
        Version.clientDistributionVersionToString(v),
      (v) =>
        Version.parseClientDistributionVersion(v),
      (desiredVersionsDeltas) =>
        changeDesiredVersions({ variables: { versions: desiredVersionsDeltas }}),
      () => {
        setTimestamp(new Date())
      },
      () => {
        Promise.all([getDesiredVersionsHistory(), getVersionsInfo()])
          .then(([desiredVersionsHistory, versionsInfo]) => {
            initView(desiredVersionsHistory.data.clientDesiredVersionsHistory,
              versionsInfo.data.clientVersionsInfo)
          })
      },
      classes)
    setView(view)
  }

  if (view && versionsInfo) {
    const columns = view.getBaseColumns()
    columns.push({
      name: 'installTime',
      headerName: 'Install Time',
      type: 'date',
      className: classes.timeColumn,
    })
    const rows = view.makeBaseRows().map(row => {
      const service = row.get('service')!.value as string
      const version = Version.parseClientDistributionVersion(row.get('version')!.value as string)
      const installInfo = versionsInfo.clientVersionsInfo
        .find(v => v.service == service &&
          Version.compareClientDistributionVersions(v.version, version) == 0)!.installInfo
      row.set('installTime', { value: installInfo.time })
      return row
    })
    return view.render(columns, rows)
  } else {
    return null
  }
}

export default ClientDesiredVersions;
