import React, {useRef, useState} from 'react';
import clsx from 'clsx';
import PropTypes from 'prop-types';
import { makeStyles } from '@material-ui/styles';
import {DeveloperDistributionVersion, Utils} from '../../../../common';
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
import {VersionsTable} from './VersionsTable';
import {
  ClientDesiredVersion,
  DeveloperDesiredVersion,
  DistributionConsumerInfo,
  DistributionServiceState,
  useClientDesiredVersionsLazyQuery,
  useClientDesiredVersionsQuery,
  useDeveloperDesiredVersionsQuery,
  useDistributionConsumersInfoQuery,
  useDistributionInfoQuery,
  useInstalledDesiredVersionsLazyQuery,
  useInstalledDesiredVersionsQuery,
  useServiceStatesLazyQuery
} from "../../../../generated/graphql";

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
  formControlLabel: {
    paddingLeft: '10px'
  },
  distributionSelect: {
    width: '150px'
  },
  onlyAlerts: {
    paddingRight: '2px'
  }
}));

const Versions = props => {
  const { className, ...rest } = props;
  const classes = useStyles();

  const [consumer, setConsumer] = useState()
  const [consumers, setConsumers] = useState(new Array<DistributionConsumerInfo>())
  const [clientVersions, setClientVersions] = useState(new Array<ClientDesiredVersion>())
  const [onlyAlerts, setOnlyAlerts] = useState(false)

  React.useEffect(() => {
    setClientVersions(new Array<ClientDesiredVersion>())
    getVersions(consumer)
  }, [consumer]);

  const consumersInfo = useDistributionConsumersInfoQuery()
  if (consumers.length == 0 && consumersInfo.data) {
    setConsumers(consumersInfo.data.distributionConsumersInfo)
  }

  const developerDesiredVersions = useDeveloperDesiredVersionsQuery()

  const [getClientDesiredVersions, clientDesiredVersions] = useClientDesiredVersionsLazyQuery()
  if (clientDesiredVersions.data) {
    setClientVersions(clientDesiredVersions.data.clientDesiredVersions)
  }

  const [getInstalledDesiredVersions, installedDesiredVersions] = useInstalledDesiredVersionsLazyQuery()
  if (installedDesiredVersions.data) {
    setClientVersions(installedDesiredVersions.data.installedDesiredVersions)
  }

  const [getServiceStates, serviceStates] = useServiceStatesLazyQuery()
  if (clientVersions.length == 0 && installedDesiredVersions.data) {
    setClientVersions(installedDesiredVersions.data.installedDesiredVersions)
  }

  const getVersions = (consumerDistributionName:string) => {
    if (consumerDistributionName) {
      getInstalledDesiredVersions({ variables: { distribution: consumerDistributionName} })
      getServiceStates({ variables: { distribution: consumerDistributionName} })
    } else {
      getClientDesiredVersions()
      getServiceStates({ variables: { distribution: localStorage.getItem('distributionName') } })
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
              className={classes.formControlLabel}
              control={<Select
                className={classes.distributionSelect}
                native
                onChange={(event) => setConsumer(event.target.value)}
                title='Select distribution'
                value={consumer}
              >
                { consumers.map( distribution => <option key={distribution}>{distribution}</option> ) }
              </Select>}
              label='Client'
            />
            <FormControlLabel
              className={classes.formControlLabel}
              control={<Checkbox
                checked={onlyAlerts}
                className={classes.onlyAlerts}
                onChange={event => setOnlyAlerts(event.target.checked)}
              />}
              label='Only Alerts'
            />
            <FormControlLabel
              className={classes.formControlLabel}
              control={<Button
                onClick={() => getConsumerVersions(consumer)}
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
        title={onlyAlerts ? 'Version Alerts' : 'Versions'}
      />
      <Divider />
      <CardContent className={classes.content}>
        <div className={classes.inner}>
          <VersionsTable
            client={consumer}
            clientVersions={clientVersions}
            developerVersions={developerVersions}
            instanceVersions={serviceStates}
            onlyAlerts={onlyAlerts}
          />
        </div>
      </CardContent>
    </Card>
  );
};

Versions.propTypes = {
  className: PropTypes.string
};

export default Versions;
