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
import {DesiredVersionsView, useBaseStyles} from "../DesiredVersionsView";
import Alert from "@material-ui/lab/Alert";

const useStyles = makeStyles((theme:any) => ({
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
  const baseClasses = useBaseStyles()
  const classes = useStyles()

  const {data: desiredVersionsHistory, refetch: getDesiredVersionsHistory} =
      useClientDesiredVersionsHistoryQuery({
    variables: {limit: 25},
    onCompleted(desiredVersionsHistory) {
      if (versionsInfo?.clientVersionsInfo) {
        initView(desiredVersionsHistory.clientDesiredVersionsHistory, versionsInfo.clientVersionsInfo)
      }
    },
    onError(err) {
      setError('Query desired versions history error ' + err.message)
    }
  })
  const {data: versionsInfo, refetch: getVersionsInfo} = useClientVersionsInfoQuery({
    onCompleted(versionsInfo) {
      if (desiredVersionsHistory?.clientDesiredVersionsHistory?.length) {
        initView(desiredVersionsHistory.clientDesiredVersionsHistory, versionsInfo.clientVersionsInfo)
      }
    },
    onError(err) {
      setError('Query client versions error ' + err.message)
    },
  })

  const [changeDesiredVersions] = useSetClientDesiredVersionsMutation()

  const [ view, setView ] = useState<DesiredVersionsView<ClientDistributionVersion>>()
  const [ version, setTimestamp ] = useState(new Date())

  const [ error, setError ] = useState<string>()

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
      baseClasses)
    setView(view)
  }

  let viewRender: JSX.Element | null = null

  if (view && versionsInfo) {
    const columns = view.getBaseColumns()
    columns.push({
      name: 'installTime',
      headerName: 'Install Time',
      type: 'date',
      className: baseClasses.timeColumn,
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
    viewRender = view.render(columns, rows)
  }
  return (<>
    {viewRender}
    {error ? <Alert className={classes.alert} severity="error">{error}</Alert> : null}
  </>)
}

export default ClientDesiredVersions;
