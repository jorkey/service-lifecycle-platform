import React, {useEffect, useState} from 'react';

import {RouteComponentProps} from "react-router-dom"
import { makeStyles } from '@material-ui/core/styles';
import {
  ClientDesiredVersion, ClientDistributionVersion,
  useClientDesiredVersionsHistoryQuery,
  useClientDesiredVersionsQuery, useClientVersionsInfoQuery,
  useSetClientDesiredVersionsMutation,
} from "../../../../generated/graphql";
import {Version} from "../../../../common";
import {DesiredVersionsView} from "../../DesiredVersionsView";

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

  const [ version, setTimestamp ] = useState(new Date())

  const [ view, setView ] = useState(new DesiredVersionsView<ClientDistributionVersion>(
    'Client Desired Versions',
    (service, version) => {
      const buildInfo = versionsInfo?.clientVersionsInfo.find(
        v => v.service == service)?.buildInfo
      return buildInfo ? { author: buildInfo.author,
        buildTime: buildInfo.time.toLocaleDateString(), comment: buildInfo.comment } : undefined
    },
    (v1, v2) =>
      Version.compareClientDistributionVersions(v1, v2),
    (v) =>
      Version.clientDistributionVersionToString(v),
    (v) =>
      Version.parseClientDistributionVersion(v),
    (desiredVersions) =>
      changeDesiredVersions({ variables: { versions: desiredVersions }}),
    () => {
      setTimestamp(new Date())
    },
    () => {
      getDesiredVersions()
      getDesiredVersionsHistory()
      getVersionsInfo()
    },
    classes))

  useEffect(() => {
    const columns = view.getBaseColumns()
    columns.push({
      name: 'installTime',
      headerName: 'Install Time',
      type: 'date',
      className: classes.timeColumn,
    })
    view.setColumns(columns)
  })

  const {data: desiredVersions, refetch: getDesiredVersions} = useClientDesiredVersionsQuery({
    onCompleted(versions) {
      view.setDesiredVersions(versions.clientDesiredVersions)
    },
    onError(err) {
      view.setError('Query desired versions error ' + err.message)
    }
  })
  const {data: desiredVersionsHistory, refetch: getDesiredVersionsHistory} = useClientDesiredVersionsHistoryQuery({
    variables: {limit: 25},
    onCompleted(versions) {
      view.setDesiredVersionsHistory(versions.clientDesiredVersionsHistory)
    },
    onError(err) {
      view.setError('Query desired versions history error ' + err.message)
    }
  })
  const {data: versionsInfo, refetch: getVersionsInfo} = useClientVersionsInfoQuery({
    onCompleted(versions) {
      view.setVersionsHistory(versions.clientVersionsInfo.map(
        v => ({ service: v.service, version: v.version } as ClientDesiredVersion)
      ))
    },
    onError(err) {
      view.setError('Query client versions error ' + err.message)
    },
  })

  const [changeDesiredVersions] = useSetClientDesiredVersionsMutation()

  if (view.isDataReady() && versionsInfo?.clientVersionsInfo) {
    const rows = view.makeBaseRows()
    rows.map(row => {
      const service = row.get('service')!.value as string
      const installInfo = versionsInfo.clientVersionsInfo.find(v => v.service == service)!.installInfo
      row.set('installTime', { value: installInfo.time })
      return row
    })
    return view.render(rows)
  } else {
    return null
  }
}

export default ClientDesiredVersions;
