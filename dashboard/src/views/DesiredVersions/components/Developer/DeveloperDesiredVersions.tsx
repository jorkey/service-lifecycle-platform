import React, {useState} from 'react';

import {RouteComponentProps} from "react-router-dom"
import { makeStyles } from '@material-ui/core/styles';
import {
  DeveloperDistributionVersion, DeveloperVersionInfo, TimedDeveloperDesiredVersions,
  useDeveloperDesiredVersionsHistoryQuery,
  useDeveloperVersionsInfoQuery,
  useSetDeveloperDesiredVersionsMutation,
} from "../../../../generated/graphql";
import {Version} from "../../../../common";
import {DesiredVersionsView, useBaseStyles} from "../../DesiredVersionsView";
import Alert from "@material-ui/lab/Alert";

const useStyles = makeStyles((theme:any) => ({
  alert: {
    marginTop: 25
  }
}));

interface DeveloperDesiredVersionsRouteParams {
}

interface DeveloperDesiredVersionsParams extends RouteComponentProps<DeveloperDesiredVersionsRouteParams> {
  fromUrl: string
}

const DeveloperDesiredVersions = (props: DeveloperDesiredVersionsParams) => {
  const baseClasses = useBaseStyles()
  const classes = useStyles()

  const {data: desiredVersionsHistory, refetch: getDesiredVersionsHistory} =
      useDeveloperDesiredVersionsHistoryQuery({
    variables: {limit: 25},
    onCompleted(desiredVersionsHistory) {
      if (versionsInfo) {
        initView(desiredVersionsHistory.developerDesiredVersionsHistory, versionsInfo.developerVersionsInfo)
      }
    },
    onError(err) {
      setError('Query desired versions history error ' + err.message)
    }
  })
  const {data: versionsInfo, refetch: getVersionsInfo} = useDeveloperVersionsInfoQuery({
    onCompleted(versionsInfo) {
      if (desiredVersionsHistory) {
        initView(desiredVersionsHistory.developerDesiredVersionsHistory, versionsInfo.developerVersionsInfo)
      }
    },
    onError(err) {
      setError('Query developer versions error ' + err.message)
    },
  })

  const [changeDesiredVersions] = useSetDeveloperDesiredVersionsMutation()

  const [ view, setView ] = useState<DesiredVersionsView<DeveloperDistributionVersion>>()
  const [ version, setTimestamp ] = useState(new Date())

  const [ error, setError ] = useState<string>()

  const initView = (desiredVersionsHistory: TimedDeveloperDesiredVersions[], versionsInfo: DeveloperVersionInfo[]) => {
    const view = new DesiredVersionsView<DeveloperDistributionVersion>(
      'Developer Desired Versions',
      desiredVersionsHistory,
      versionsInfo.map(v => { return { version: v,
        info: { author: v.buildInfo.author, buildTime: v.buildInfo.time.toLocaleString(), comment: v.buildInfo.comment }}}),
      (v1, v2) =>
        Version.compareDeveloperDistributionVersions(v1, v2),
      (v) =>
        Version.developerDistributionVersionToString(v),
      (v) =>
        Version.parseDeveloperDistributionVersion(v),
      (desiredVersionsDeltas) =>
        changeDesiredVersions({ variables: { versions: desiredVersionsDeltas }}),
      () => {
        setTimestamp(new Date())
      },
      () => {
        Promise.all([getDesiredVersionsHistory(), getVersionsInfo()])
          .then(([desiredVersionsHistory, versionsInfo]) => {
            initView(desiredVersionsHistory.data.developerDesiredVersionsHistory,
              versionsInfo.data.developerVersionsInfo)
          })
      },
      baseClasses)
    setView(view)
  }

  let viewRender: JSX.Element | null = null

  if (view && versionsInfo) {
    const columns = view.getBaseColumns()
    const rows = view.makeBaseRows()
    viewRender = view.render(columns, rows)
  }
  return (<>
    {viewRender}
    {error ? <Alert className={classes.alert} severity="error">{error}</Alert> : null}
  </>)
}

export default DeveloperDesiredVersions;
