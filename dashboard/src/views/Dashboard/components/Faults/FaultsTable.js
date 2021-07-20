import {Version} from '../../../../common';
import {Table, TableBody, TableCell, TableHead, TableRow} from '@material-ui/core';
import React from 'react';
import {makeStyles} from '@material-ui/styles';
import Typography from '@material-ui/core/Typography';
import {Info} from './ServiceState';

// eslint-disable-next-line no-unused-vars
const useStyles = makeStyles(theme => ({
  serviceColumn: {
    width: '150px',
    padding: '4px',
    paddingLeft: '16px'
  },
  versionColumn: {
    padding: '4px',
    width: '100px'
  },
  alarmVersionColumn: {
    padding: '4px',
    width: '100px',
    color: 'red'
  },
  directoryColumn: {
    padding: '4px',
    width: '300px'
  },
  instancesColumn: {
    padding: '4px'
  },
  infoColumn: {
    width: '30px',
    padding: '4px',
    paddingRight: '10px'
  }
}));

export const ServiceVersions = props => {
  const { client, service, developerVersion, clientVersions, instanceVersions, onlyAlerts } = props;
  const classes = useStyles();
  const clientVersion = clientVersions.get(service)
  const versions = instanceVersions.get(service) ? Object.entries(instanceVersions.get(service)) : []
  let versionIndex = versions.length, version = undefined
  let directories = [], directoryIndex = 0, directory = undefined
  let instances = [], instanceIndex = 0, instance = undefined
  let rows = []
  let rowsStack = []
  let alertService = false
  for (let rowNum=0, versionRowNum=0; ; rowNum++, versionRowNum++) {
    instanceIndex--
    if (instanceIndex < 0) {
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
          const [ver, dirs] = versions[versionIndex]
          version = ver
          directories = Object.entries(dirs)
        } else {
          version = undefined
          directories = []
        }
        directoryIndex = directories.length - 1
        versionRowNum = 0
      }
      if (directoryIndex >= 0) {
        const [dir, inst] = directories[directoryIndex]
        directory = dir
        instances = Object.entries(inst)
      } else {
        directory = undefined
        instances = []
      }
      instanceIndex = instances.length - 1
    }
    if (instanceIndex >= 0) {
      [, instance] = instances[instanceIndex]
    } else {
      instance = undefined
    }
    let clientVersionAlarm = clientVersion && Version.compare(clientVersion, developerVersion, false)
    let workingVersionAlarm = version && clientVersion && Version.compare(version, clientVersion, true)
    alertService = alertService || clientVersionAlarm || workingVersionAlarm
    rowsStack.push(<TableRow
      hover
      key={service + '-' + rowNum}
    >
      {(versionIndex <= 0 && directoryIndex <= 0 && instanceIndex <= 0) ? (
        <>
          <TableCell
            className={classes.serviceColumn}
            rowSpan={rowNum + 1}
          >{service}</TableCell>
          <TableCell
            className={classes.versionColumn}
            rowSpan={rowNum + 1}
          >{developerVersion}</TableCell>
          { client != 'distribution' ?
            <TableCell
              className={!clientVersionAlarm ? classes.versionColumn : classes.alarmVersionColumn}
              rowSpan={rowNum + 1}
            >{clientVersion}</TableCell> : null}
        </>)
        : null
      }
      {version ?
        (directoryIndex == 0 && instanceIndex == 0) ? (
          <TableCell
            className={!workingVersionAlarm ? classes.versionColumn : classes.alarmVersionColumn}
            rowSpan={versionRowNum + 1}
          >
            {version}
          </TableCell>)
          : null
        : <TableCell className={classes.versionColumn}/>
      }
      {directory ?
        instanceIndex == 0 ? (
          <TableCell
            className={classes.directoryColumn}
            rowSpan={instances.length}
          >{directory}</TableCell>)
          : null
        : <TableCell className={classes.directoryColumn}/>}
      {instance ?
        <TableCell className={classes.instancesColumn}>
          <Typography>{instance}</Typography>
        </TableCell> : <TableCell className={classes.instancesColumn}/>}
      {instance ?
        <TableCell className={classes.infoColumn}>
          <Info
            alert={workingVersionAlarm}
            client={client}
            directory={directory}
            instance={instance}
            service={service}
          />
        </TableCell> : <TableCell className={classes.infoColumn}/>}
    </TableRow>)
  }
  return rows
}

export const FaultsTable = props => {
  const {client, developerVersions, clientVersions, instanceVersions, onlyAlerts} = props;
  const classes = useStyles();

  return client ? (<Table stickyHeader>
    <TableHead>
      <TableRow>
        <TableCell className={classes.serviceColumn}>Service</TableCell>
        <TableCell className={classes.versionColumn}>Developer Version</TableCell>
        { client != 'distribution' ? <TableCell className={classes.versionColumn}>Client Version</TableCell> : null }
        <TableCell className={classes.versionColumn}>Working Version</TableCell>
        <TableCell className={classes.directoryColumn}>Directory</TableCell>
        <TableCell className={classes.instancesColumn}>Instances</TableCell>
        <TableCell className={classes.infoColumn}>Info</TableCell>
      </TableRow>
    </TableHead>
    <TableBody>
      { developerVersions.sort().map(([service, developerVersion]) =>
        <ServiceVersions
          client={client}
          clientVersions={clientVersions}
          developerVersion={developerVersion}
          instanceVersions={instanceVersions}
          key={service}
          onlyAlerts={onlyAlerts}
          service={service}
        />) }
    </TableBody>
  </Table>) : null
}
