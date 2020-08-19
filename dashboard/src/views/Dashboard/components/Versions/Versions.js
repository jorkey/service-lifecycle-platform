import React, { useState } from 'react';
import clsx from 'clsx';
import PropTypes from 'prop-types';
import { makeStyles } from '@material-ui/styles';
import { Utils } from '../../../../utils';
import {
  Card,
  CardActions,
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
  Tooltip,
  TableSortLabel,
  Select
} from '@material-ui/core';
import Grid from "@material-ui/core/Grid";

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
  instancesColumn: {
    padding: '6px'
  },
  clientSelect: {
    width: '100px'
  }
}));

const Versions = props => {
  const { className, ...rest } = props;

  const classes = useStyles();

  const [client, setClient] = useState()
  const [clients, setClients] = useState([])
  const [desiredVersions, setDesiredVersions] = useState([])
  const [instanceVersions, setInstanceVersions] = useState(new Map())

  React.useEffect(() => {
    Utils.getClients().then(clients => setClients(clients))
  }, [])

  React.useEffect(() => {
    setInstanceVersions(new Map())
    Utils.getDesiredVersions(client).then(versions => setDesiredVersions(Object.entries(versions)))
    if (client) {
      Utils.getInstanceVersions(client).then(versions => {
        setInstanceVersions(new Map(Object.entries(versions))) })
    }
  }, [client]);

  const ServiceVersions = props => {
    const { service, desiredVersion } = props;

    const concatInstances = (instances) => {
      let result = "";
      instances.sort().forEach(([index, instance]) => {
        if (result) result += ", "
        result += instance
      })
      return result
    }

    if (!client) {
      return (
        <TableRow hover key={service}>
          <TableCell className={classes.serviceColumn}>{service}</TableCell>
          <TableCell className={classes.versionColumn}>{desiredVersion}</TableCell>
        </TableRow>
      )
    } else {
      const versions = instanceVersions.get(service)
      if (versions && Object.entries(versions).length > 0) {
        return Object.entries(versions).map(([version, instances], index) => {
          return (<TableRow hover key={service}>
            { index == 0 ? (
            <>
              <TableCell className={classes.serviceColumn} rowSpan={versions.length}>{service}</TableCell>
              <TableCell className={classes.versionColumn} rowSpan={versions.length}>{desiredVersion}</TableCell>
            </>) : null
            }
            <TableCell className={classes.instancesColumn}>
              {version}
            </TableCell>
            <TableCell className={classes.instancesColumn}>
              <div>{concatInstances(Object.entries(instances))}</div>
            </TableCell>
          </TableRow>)
        })
      } else {
        return (
          <TableRow hover key={service}>
            <TableCell className={classes.serviceColumn}>{service}</TableCell>
            <TableCell className={classes.versionColumn}>{desiredVersion}</TableCell>
            <TableCell className={classes.instancesColumn}/>
            <TableCell className={classes.instancesColumn}/>
          </TableRow>
        )
      }
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
                { client ? <TableCell className={classes.versionColumn}>Client Versions</TableCell> : <TableCell/> }
                { client ? <TableCell className={classes.instancesColumn}>Client Instances</TableCell> : <TableCell/> }
              </TableRow>
            </TableHead>
            <TableBody>
              { desiredVersions.sort().map(([service, version]) => <ServiceVersions key={service} service={service} desiredVersion={version}/>) }
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
