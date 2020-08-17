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
  desiredVersionColumn: {
    padding: '6px'
  }
}));

const Versions = props => {
  const { className, ...rest } = props;

  const classes = useStyles();

  const [client, setClient] = useState()
  const [clients, setClients] = useState([])
  const [desiredVersions, setDesiredVersions] = useState([])

  React.useEffect(() => {
    Utils.getClients().then(clients => setClients(clients))
    Utils.getDesiredVersions(client).then(versions => setDesiredVersions(Object.entries(versions)))
  }, [client]);

  console.log("render " + desiredVersions.length)

  return (
    <Card
      {...rest}
      className={clsx(classes.root, className)}
    >
      <CardHeader
        action={
          <Grid>
            <InputLabel htmlFor="age-native-simple">Client</InputLabel>
            <Select
              native
              value={client}
              onChange={(event) => {
                setClient(event.target.value);
              }}
              >
              {clients.map( client => <option>{client}</option> )}
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
                <TableCell className={classes.desiredVersionColumn}>Desired version</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {desiredVersions.map(([service, version]) =>
                (
                  <TableRow
                    hover
                    key={service}
                  >
                    <TableCell className={classes.serviceColumn}>{service}</TableCell>
                    <TableCell className={classes.desiredVersionColumn}>{version}</TableCell>
                  </TableRow>
                ))}
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
