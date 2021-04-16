import React, {useState} from 'react';
import clsx from 'clsx';
import PropTypes from 'prop-types';
import { makeStyles } from '@material-ui/styles';
import {
  Card,
  CardHeader,
  CardContent,
  Button,
  Divider,
  InputLabel,
  Select
} from '@material-ui/core';
import RefreshIcon from '@material-ui/icons/Refresh';
import FormGroup from '@material-ui/core/FormGroup';
import FormControlLabel from '@material-ui/core/FormControlLabel';
import Checkbox from '@material-ui/core/Checkbox';
import {FaultsTable} from './FaultsTable';

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
  control: {
    paddingLeft: '10px'
  },
  clientSelect: {
    width: '100px'
  },
  onlyAlerts: {
    paddingRight: '2px'
  }
}));

const Faults = props => {
  const { className, ...rest } = props;
  const classes = useStyles();

  const [client, setClient] = useState()
  const [clients, setClients] = useState([])

  const [service, setService] = useState()
  const [services, setServices] = useState([])

  React.useEffect(() => {
    Utils.getClients().then(clients => {
      setClients(clients)
      if (clients.length) {
        setClient(clients[0])
      }
    })
  }, [])

  React.useEffect(() => {
    setDeveloperVersions([])
    setClientVersions(new Map())
    setInstanceVersions(new Map())
    getClientVersions(client)
  }, [client]);

  const getClientVersions = (client) => {
    if (client) {
      Utils.getDesiredVersions(client).then(versions => {
        setDeveloperVersions(Object.entries(versions))
        if (client == 'distribution') {
          setClientVersions(new Map(Object.entries(versions)))
        }
      })
      Utils.getInstanceVersions(client).then(versions => {
        setInstanceVersions(new Map(Object.entries(versions)))
      })
      if (client != 'distribution') {
        Utils.getInstalledDesiredVersions(client).then(versions => {
          setClientVersions(new Map(Object.entries(versions)))
        })
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
          <FormGroup row>
            <FormControlLabel
              className={classes.control}
              control={<Select
                className={classes.clientSelect}
                native
                onChange={(event) => setClient(event.target.value)}
                title='Select client'
                value={client}
              >
                { clients.map( client => <option key={client}>{client}</option> ) }
              </Select>}
              label='Client'
            />
            <FormControlLabel
              className={classes.control}
              control={<Checkbox
                checked={onlyAlerts}
                className={classes.onlyAlerts}
                onChange={event => setOnlyAlerts(event.target.checked)}
              />}
              label='Only Alerts'
            />
            <FormControlLabel
              className={classes.control}
              control={<Button
                onClick={() => getClientVersions(client)}
                title='Refresh'
              >
                <RefreshIcon/>
                <InputLabel>{new Date().getHours().toLocaleString(undefined, {minimumIntegerDigits: 2}) +
                  ':' + new Date().getMinutes().toLocaleString(undefined, {minimumIntegerDigits: 2}) +
                  ':' + new Date().getSeconds().toLocaleString(undefined, {minimumIntegerDigits: 2})}</InputLabel>
              </Button>}
            />
          </FormGroup>
        }
        title={onlyAlerts ? 'Version Alerts' : 'Faults'}
      />
      <Divider />
      <CardContent className={classes.content}>
        <div className={classes.inner}>
          <FaultsTable
            client={client}
            clientVersions={clientVersions}
            developerVersions={developerVersions}
            instanceVersions={instanceVersions}
            onlyAlerts={onlyAlerts}
          />
        </div>
      </CardContent>
    </Card>
  );
};

Faults.propTypes = {
  className: PropTypes.string
};

export default Faults;
