import {Version} from '../../../../common';
import {Table, TableBody, TableCell, TableHead, TableRow} from '@material-ui/core';
import React from 'react';
import {makeStyles} from '@material-ui/styles';
import Typography from '@material-ui/core/Typography';
import {Info} from './ServiceState';
import {
  ClientDesiredVersion, ClientDistributionVersion,
  DeveloperDesiredVersion,
  DeveloperDistributionVersion, DistributionServiceState,
} from "../../../../generated/graphql";

// eslint-disable-next-line no-unused-vars
const useStyles = makeStyles(theme => ({
  serviceColumn: {
    width: '150px',
  },
  versionColumn: {
    width: '175px'
  },
  alarmVersionColumn: {
    width: '150px',
    color: 'red'
  },
  directoryColumn: {
    width: '300px'
  },
  instancesColumn: {
  },
  infoColumn: {
    width: '30px',
    paddingRight: '10px'
  }
}));

interface ServiceVersionsProps {
  service: string
  developerVersion: DeveloperDistributionVersion
  clientVersion: ClientDistributionVersion|undefined
  serviceStates: Array<DistributionServiceState>|undefined
  onlyAlerts: Boolean
}

export const ServiceVersions: React.FC<ServiceVersionsProps> = props => {
  const { service, developerVersion, clientVersion, serviceStates, onlyAlerts } = props;
  const classes = useStyles();
  let versionsTree = new Array<[ClientDistributionVersion, Array<[string, Array<DistributionServiceState>]>]>()
  serviceStates?.forEach(state => {
    if (state.state.version) {
      let versionNode = versionsTree.find(node => node[0] === state.state.version)
      if (!versionNode) {
        versionNode = [state.state.version, new Array<[string, Array<DistributionServiceState>]>()]
        versionsTree.push(versionNode)
      }
      let directoriesNode = versionNode[1]
      let directoryNode = directoriesNode.find(node => node[0] === state.directory)
      if (!directoryNode) {
        directoryNode = [state.directory, new Array<DistributionServiceState>()]
        directoriesNode.push(directoryNode)
      }
      directoryNode[1].push(state)
    } })
  let versionIndex = versionsTree.length, version:ClientDistributionVersion|undefined = undefined
  let directories = new Array<[string, Array<DistributionServiceState>]>(), directoryIndex = 0, directory:string|undefined = undefined
  let states = new Array<DistributionServiceState>(), stateIndex = 0, state:DistributionServiceState|undefined = undefined
  let rows = []
  let rowsStack = []
  let alertService = false
  for (let rowNum=0, versionRowNum=0; ; rowNum++, versionRowNum++) {
    stateIndex--
    if (stateIndex < 0) {
      directoryIndex--
      if (directoryIndex < 0) {
        versionIndex--
        if (rowNum && versionIndex < 0) {
          if (!onlyAlerts || alertService) {
            while (rowsStack.length) {
              rows.push(rowsStack.pop())
            }
          } else {
            rowsStack = []
          }
          break
        }
        if (versionIndex >= 0) {
          const node = versionsTree[versionIndex]
          version = node[0]
          directories = node[1]
        } else {
          version = undefined
          directories = []
        }
        directoryIndex = directories.length - 1
        versionRowNum = 0
      }
      if (directoryIndex >= 0) {
        const node = directories[directoryIndex]
        directory = node[0]
        states = node[1]
      } else {
        directory = undefined
        states = []
      }
      stateIndex = states.length - 1
    }
    if (stateIndex >= 0) {
      state = states[stateIndex]
    } else {
      state = undefined
    }
    let clientVersionAlarm = clientVersion !== undefined &&
      !Version.contains(clientVersion, developerVersion)
    let workingVersionAlarm = version !== undefined && clientVersion !== undefined &&
      Version.compareClientDistributionVersions(version, clientVersion) != 0
    alertService = alertService || clientVersionAlarm || workingVersionAlarm
    rowsStack.push(<TableRow
      hover
      key={service + '-' + rowNum}
    >
      {(versionIndex <= 0 && directoryIndex <= 0 && stateIndex <= 0) ? (
        <>
          <TableCell
            className={classes.serviceColumn}
            rowSpan={rowNum + 1}
          >{service}</TableCell>
          <TableCell
            className={classes.versionColumn}
            rowSpan={rowNum + 1}
          >{Version.developerDistributionVersionToString(developerVersion)}</TableCell>
          { clientVersion ?
            <TableCell
              className={!clientVersionAlarm ? classes.versionColumn : classes.alarmVersionColumn}
              rowSpan={rowNum + 1}
            >{Version.clientDistributionVersionToString(clientVersion)}</TableCell> : null}
        </>)
        : null
      }
      {version ?
        (directoryIndex === 0 && stateIndex === 0) ? (
          <TableCell
            className={!workingVersionAlarm ? classes.versionColumn : classes.alarmVersionColumn}
            rowSpan={versionRowNum + 1}
          >
            {Version.clientDistributionVersionToString(version)}
          </TableCell>)
          : null
        : <TableCell className={classes.versionColumn}/>
      }
      {directory ?
        stateIndex === 0 ? (
          <TableCell
            className={classes.directoryColumn}
            rowSpan={states.length}
          >{directory}</TableCell>)
          : null
        : <TableCell className={classes.directoryColumn}/>}
      {state ?
        <TableCell className={classes.instancesColumn}>
          <Typography>{state.instance}</Typography>
        </TableCell> : <TableCell className={classes.instancesColumn}/>}
      {state ?
        <TableCell className={classes.infoColumn}>
          <Info
            alert={workingVersionAlarm}
            serviceState={state.state}
          />
        </TableCell> : <TableCell className={classes.infoColumn}/>}
    </TableRow>)
  }
  return (<>{rows}</>)
}

interface VersionsTableProps {
  developerVersions: Array<DeveloperDesiredVersion>
  clientVersions: Array<ClientDesiredVersion>|undefined
  serviceStates: Array<DistributionServiceState>|undefined
  onlyAlerts: Boolean
}

export const VersionsTable: React.FC<VersionsTableProps> = props => {
  const {developerVersions, clientVersions, serviceStates, onlyAlerts} = props
  const classes = useStyles()

  return (<Table stickyHeader>
    <TableHead>
      <TableRow>
        <TableCell className={classes.serviceColumn}>Service</TableCell>
        <TableCell className={classes.versionColumn}>Developer Desired Version</TableCell>
        <TableCell className={classes.versionColumn}>Client Desired Version</TableCell>
        <TableCell className={classes.versionColumn}>Working Version</TableCell>
        <TableCell className={classes.directoryColumn}>Directory</TableCell>
        <TableCell className={classes.instancesColumn}>Instances</TableCell>
        <TableCell className={classes.infoColumn}>Info</TableCell>
      </TableRow>
    </TableHead>
    <TableBody>
      { [...developerVersions].sort().map(desiredVersion =>
        <ServiceVersions
          service={desiredVersion.service}
          developerVersion={desiredVersion.version}
          clientVersion={clientVersions?.find(version => version.service === desiredVersion.service)?.version}
          serviceStates={serviceStates?.filter(state => state.service === desiredVersion.service)}
          onlyAlerts={onlyAlerts}
          key={desiredVersion.service}
        />) }
    </TableBody>
  </Table>)
}
