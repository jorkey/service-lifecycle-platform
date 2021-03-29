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
  useDeveloperDesiredVersionsQuery,
  useDistributionConsumersInfoQuery,
  useDistributionInfoQuery
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
  const [consumers, setConsumers] = useState([])
  const [developerVersions, setDeveloperVersions] = useState([])
  const [clientVersions, setClientVersions] = useState(new Map())
  const [instanceVersions, setInstanceVersions] = useState(new Map())
  const [onlyAlerts, setOnlyAlerts] = useState(false)

  React.useEffect(() => {
    setDeveloperVersions([])
    setClientVersions(new Map())
    setInstanceVersions(new Map())
    getConsumerVersions(consumer)
  }, [consumer]);

  const consumersInfo = useDistributionConsumersInfoQuery();

  if (consumers.length == 0 && consumersInfo.data) {
    let consumers = new Array()
    consumersInfo.data.distributionConsumersInfo.forEach(info => consumers.push(info.distributionName))
    setConsumers(consumers)
  }

  const developerDesiredVersions = useDeveloperDesiredVersionsQuery();

  if (developerVersions.length == 0 && developerDesiredVersions.data) {
    developerDesiredVersions.data.developerDesiredVersions.forEach((version) => {
      let service = version.serviceName
      let v = new DeveloperDistributionVersion('', null)
      let version1/*:DeveloperDistributionVersion*/ = version.version
    })
  }

  const getConsumerVersions = (consumer) => {
    if (consumer) {
      Utils.getDesiredVersions(consumer).then(versions => {
        setDeveloperVersions(Object.entries(versions))
        if (consumer == 'distribution') {
          setClientVersions(new Map(Object.entries(versions)))
        }
      })
      Utils.getInstanceVersions(consumer).then(versions => {
        setInstanceVersions(new Map(Object.entries(versions)))
      })
      if (consumer != 'distribution') {
        Utils.getInstalledDesiredVersions(consumer).then(versions => {
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
            instanceVersions={instanceVersions}
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
