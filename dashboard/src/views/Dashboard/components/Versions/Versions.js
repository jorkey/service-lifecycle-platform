import React, {useRef, useState} from 'react';
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
import {Version} from "../../../../common/Version";
import RefreshIcon from "@material-ui/icons/Refresh";
import FormGroup from "@material-ui/core/FormGroup";
import FormControlLabel from "@material-ui/core/FormControlLabel";
import Checkbox from "@material-ui/core/Checkbox";

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
  formControlLabel: {
    paddingLeft: '10px'
  },
  clientSelect: {
    width: '100px'
  },
  onlyAlerts: {
    paddingRight: '2px'
  },
}));

const Versions = props => {
  const { className, ...rest } = props;

  const classes = useStyles();

  const [client, setClient] = useState()
  const [clients, setClients] = useState([])
  const [desiredVersions, setDesiredVersions] = useState([])
  const [clientVersions, setClientVersions] = useState(new Map())
  const [instanceVersions, setInstanceVersions] = useState(new Map())
  const [onlyAlerts, setOnlyAlerts] = useState(false)

  const distributionClient = false // TODO localStorage.getItem('distribution').client

  React.useEffect(() => {
    if (!distributionClient) {
      Utils.getClients().then(clients => {
        setClients(clients)
        if (clients.length) {
          setClient(clients[0])
        }
      })
    }
  }, [])

  React.useEffect(() => {
    setDesiredVersions([])
    setClientVersions(new Map())
    setInstanceVersions(new Map())
    getClientVersions(client)
  }, [client]);

  const getClientVersions = (client) => {
    if (client) {
      Utils.getDesiredVersions(client).then(versions => {
        setDesiredVersions(Object.entries(versions))
        if (client == "distribution") {
          setClientVersions(new Map(Object.entries(versions)))
        }
      })
      Utils.getInstanceVersions(client).then(versions => {
        setInstanceVersions(new Map(Object.entries(versions)))
      })
      Utils.getInstalledDesiredVersions(client).then(versions => {
        setClientVersions(new Map(Object.entries(versions)))
      })
    }
  }

  const ServiceVersions = props => {
    const { service, desiredVersion } = props;

    const clientVersion = clientVersions.get(service)

    const concatInstances = (instances) => {
      let result = "";
      instances.forEach(([index, instance]) => {
        if (result) result += ", "
        result += instance
      })
      return result
    }

    const versions = instanceVersions.get(service) ? Object.entries(instanceVersions.get(service)) : []
    let versionIndex = versions.length, version = undefined
    let directories = [], directoryIndex = 0
    let rows = []
    let rowsStack = []
    let alertService = false
    for (let rowNum=0; ; rowNum++) {
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
      }
      let directory = undefined, instances = []
      if (directoryIndex >= 0) {
        const [dir, inst] = directories[directoryIndex]
        directory = dir
        instances = Object.entries(inst)
      }
      let clientVersionAlarm = clientVersion && Version.compare(clientVersion, desiredVersion, false)
      let workingVersionAlarm = version && clientVersion && Version.compare(version, clientVersion, true)
      alertService = alertService || clientVersionAlarm || workingVersionAlarm
      rowsStack.push(<TableRow hover key={service + "-" + rowNum}>
        {(versionIndex <= 0 && directoryIndex <= 0) ? (
            <>
              <TableCell className={classes.serviceColumn} rowSpan={rowNum + 1}>{service}</TableCell>
              <TableCell className={classes.versionColumn} rowSpan={rowNum + 1}>{desiredVersion}</TableCell>
              { (!distributionClient && client != "distribution") ?
                <TableCell className={!clientVersionAlarm ? classes.versionColumn : classes.alarmVersionColumn}
                           rowSpan={rowNum + 1}>{clientVersion}</TableCell> : null}
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
  }

  return (
    <Card
      {...rest}
      className={clsx(classes.root, className)}
    >
      <CardHeader
        action={
          <FormGroup row>
            <FormControlLabel
              label="Client"
              className={classes.formControlLabel}
              control={ <Select
                    title="Select client"
                    className={classes.clientSelect}
                    native
                    value={client}
                    onChange={(event) => {
                      setClient(event.target.value);
                    }}
                  >
                  { clients.map( client => <option key={client}>{client}</option> ) }
                </Select> }
            />
            <FormControlLabel
              label="Only Alerts"
              className={classes.formControlLabel}
              control={ <Checkbox
                className={classes.onlyAlerts}
                checked={onlyAlerts}
                onChange={event => setOnlyAlerts(event.target.checked)}
              /> }
            />
            <FormControlLabel
              className={classes.formControlLabel}
              control={ <Button title="Refresh" onClick={() => getClientVersions(client)}>
                <RefreshIcon/>
                <InputLabel>{new Date().getHours().toLocaleString(undefined, {minimumIntegerDigits: 2}) +
                  ":" + new Date().getMinutes().toLocaleString(undefined, {minimumIntegerDigits: 2}) +
                  ":" + new Date().getSeconds().toLocaleString(undefined, {minimumIntegerDigits: 2})}</InputLabel>
              </Button> }
            />
          </FormGroup>
        }
        title={onlyAlerts ? "Version Alerts" : "Versions" }
      />
      <Divider />
      <CardContent className={classes.content}>
        <div className={classes.inner}>
          { distributionClient || client ?
            <Table>
              <TableHead>
                <TableRow>
                  <TableCell className={classes.serviceColumn}>Service</TableCell>
                  <TableCell className={classes.versionColumn}>Desired Version</TableCell>
                  { (!distributionClient && client != "distribution") ? <TableCell className={classes.versionColumn}>Client Version</TableCell> : null }
                  <TableCell className={classes.versionColumn}>Working Version</TableCell>
                  <TableCell className={classes.directoryColumn}>Directory</TableCell>
                  <TableCell className={classes.instancesColumn}>Instances</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                { desiredVersions.sort().map(([service, desiredVersion]) =>
                    <ServiceVersions key={service} service={service} desiredVersion={desiredVersion}/>) }
              </TableBody>
            </Table> : null }
        </div>
      </CardContent>
    </Card>
  );
};

Versions.propTypes = {
  className: PropTypes.string
};

export default Versions;
