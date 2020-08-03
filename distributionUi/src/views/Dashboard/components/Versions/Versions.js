import React, { useState } from 'react';
import clsx from 'clsx';
import PropTypes from 'prop-types';
import { makeStyles } from '@material-ui/core/styles';
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
  }
}));

const Versions = props => {
  const { className, ...rest } = props;

  const classes = useStyles();

  const [client, setClient] = useState()
  const [clients, setClients] = useState([])
  const [desiredVersions, setDesiredVersions] = useState([])

  React.useEffect(() => {
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
          <>
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
          </>
        }

        title="Versions"
      />
      <Divider />
      <CardContent className={classes.content}>
        <div className={classes.inner}>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Desired version</TableCell>
                <TableCell>Instance version</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {desiredVersions.map(([service, version]) =>
                (
                  <TableRow
                    hover
                    key={service}
                  >
                    <TableCell>{service}</TableCell>
                    <TableCell>{version}</TableCell>
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
