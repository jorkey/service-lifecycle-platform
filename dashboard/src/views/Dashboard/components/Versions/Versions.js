import React, { useState } from 'react';
import clsx from 'clsx';
import PropTypes from 'prop-types';
import { makeStyles } from '@material-ui/styles';
import { Utils } from '../../../../common';
import {
  Card,
  CardHeader,
  CardContent,
  Button,
  Divider,
  InputLabel,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Select
} from '@material-ui/core';
import Grid from "@material-ui/core/Grid";
import {Version} from "../../../../common/Version";
import RefreshIcon from "@material-ui/icons/Refresh";

const useStyles = makeStyles(theme => ({
  root: {},
  content: {
    padding: 0
  },
  inner: {
    minWidth: 800
  },
  statusContainer: {
    display: 'flex',
    alignItems: 'center'
  },
  status: {
    marginRight: theme.spacing(1)
  },
  actions: {
    justifyContent: 'flex-end'
  },
  serviceColumn: {
    width: '200px',
    padding: '6px',
    paddingLeft: '16px'
  },
  versionColumn: {
    padding: '6px',
    width: '200px'
  },
  alarmVersionColumn: {
    padding: '6px',
    width: '200px',
    color: 'red'
  },
  directoryColumn: {
    padding: '6px',
    width: '300px'
  },
  instancesColumn: {
    padding: '6px'
  },
  clientSelect: {
    width: '100px'
  },
  refresh: {
    paddingLeft: '10px',
    paddingRight: '10px'
  }
}));

const Versions = props => {
  const { className, ...rest } = props;

  const classes = useStyles();

  const [client, setClient] = useState()
  const [clients, setClients] = useState([])
  const [desiredVersions, setDesiredVersions] = useState([])
  const [installedDesiredVersions, setInstalledDesiredVersions] = useState(new Map())
  const [instanceVersions, setInstanceVersions] = useState(new Map())

  React.useEffect(() => {
    Utils.getClients().then(clients => setClients(clients))
  }, [])

  React.useEffect(() => {
    setInstalledDesiredVersions(new Map())
    setInstanceVersions(new Map())
    getVersions(client)
  }, [client]);

  const getVersions = (client) => {
    Utils.getDesiredVersions(client).then(versions => {
      setDesiredVersions(Object.entries(versions))
      if (!client) {
        setInstalledDesiredVersions(new Map(Object.entries(versions)))
      }
    })
    Utils.getInstanceVersions(client).then(versions => {
      setInstanceVersions(new Map(Object.entries(versions))) })
    if (client) {
      Utils.getInstalledDesiredVersions(client).then(versions => {
        setInstalledDesiredVersions(new Map(Object.entries(versions)))
      })
    }
  }

  const ServiceVersions = props => {
    const { service, desiredVersion } = props;

    const installedVersion = installedDesiredVersions.get(service)

    const concatInstances = (instances) => {
      let result = "";
      instances.forEach(([index, instance]) => {
        if (result) result += ", "
        result += instance
      })
      return result
    }

    if (!client || service != "builder") {
      const versions = instanceVersions.get(service) ? Object.entries(instanceVersions.get(service)) : []
      let versionIndex = versions.length, version = undefined
      let directories = [], directoryIndex = 0
      let rows = []
      let rowsStack = []
      let alarmedService = false
      for (let rowNum=0; ; rowNum++) {
        directoryIndex--
        if (directoryIndex < 0) {
          versionIndex--
          if (rowNum && versionIndex < 0) {
            if (alarmedService) {
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
        }
        let directory = undefined, instances = []
        if (directoryIndex >= 0) {
          const [dir, inst] = directories[directoryIndex]
          directory = dir
          instances = Object.entries(inst)
        }
        let installedVersionAlarm = installedVersion && Version.compare(installedVersion, desiredVersion, false)
        let workingVersionAlarm = version && installedVersion && Version.compare(version, installedVersion, true)
        alarmedService = alarmedService || installedVersionAlarm || workingVersionAlarm
        rowsStack.push(<TableRow hover key={service + "-" + rowNum}>
          {(versionIndex <= 0 && directoryIndex <= 0) ? (
              <>
                <TableCell className={classes.serviceColumn} rowSpan={rowNum + 1}>{service}</TableCell>
                <TableCell className={classes.versionColumn} rowSpan={rowNum + 1}>{desiredVersion}</TableCell>
                {client ?
                  <TableCell className={!installedVersionAlarm ? classes.versionColumn : classes.alarmVersionColumn}
                             rowSpan={rowNum + 1}>{installedVersion}</TableCell> : null}
              </>)
            : null
          }
          {version ?
            directoryIndex == 0 ? (
                <TableCell className={!workingVersionAlarm ? classes.versionColumn : classes.alarmVersionColumn}
                           rowSpan={Math.max(directories.length, 1)}>
                  {version}
                </TableCell>)
              : null
            : <TableCell className={classes.versionColumn}/>
          }
          {directory ?
            <TableCell className={classes.directoryColumn}>{directory}</TableCell>
            : <TableCell className={classes.directoryColumn}/>}
          {instances.length != 0 ?
            <TableCell className={classes.instancesColumn}>
              <div>{concatInstances(instances)}</div>
            </TableCell>
            : <TableCell className={classes.instancesColumn}/>
          }
        </TableRow>)
      }
      return rows
    } else {
      return null
    }
  }

  return (
    <Card
      {...rest}
      className={clsx(classes.root, className)}
    >
      <CardHeader
        action={
          <Grid>
            <InputLabel>Client</InputLabel>
            <Select
              title="Select client"
              className={classes.clientSelect}
              native
              value={client}
              onChange={(event) => {
                setClient(event.target.value);
              }}
              >
              <option aria-label="" />
              { clients.map( client => <option key={client}>{client}</option> ) }
            </Select>
            <Button title="Refresh" className={classes.refresh} onClick={() => getVersions(client)}>
              <RefreshIcon/>
              <InputLabel>{new Date().getHours().toLocaleString(undefined, {minimumIntegerDigits: 2}) +
                ":" + new Date().getMinutes().toLocaleString(undefined, {minimumIntegerDigits: 2}) +
                ":" + new Date().getSeconds().toLocaleString(undefined, {minimumIntegerDigits: 2})}</InputLabel>
            </Button>
          </Grid>
        }
        title="Versions"
      />
      <Divider />
      <CardContent className={classes.content}>
        <div className={classes.inner}>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell className={classes.serviceColumn}>Service</TableCell>
                <TableCell className={classes.versionColumn}>Desired Version</TableCell>
                { client ? <TableCell className={classes.versionColumn}>Installed Version</TableCell> : null }
                <TableCell className={classes.versionColumn}>Working Version</TableCell>
                <TableCell className={classes.directoryColumn}>Directory</TableCell>
                <TableCell className={classes.instancesColumn}>Instances</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              { desiredVersions.sort().map(([service, desiredVersion]) =>
                  <ServiceVersions key={service} service={service} desiredVersion={desiredVersion}/>) }
            </TableBody>
          </Table>
        </div>
      </CardContent>
    </Card>
  );
};

Versions.propTypes = {
  className: PropTypes.string
};

export default Versions;
