import React, {useRef, useState} from 'react';
import clsx from 'clsx';
import { makeStyles } from '@material-ui/styles';
import {Utils} from '../../../../common';
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
  ClientDesiredVersion, DeveloperDesiredVersion,
  useClientDesiredVersionsLazyQuery,
  useDeveloperDesiredVersionsLazyQuery,
  useDistributionConsumersInfoQuery,
  useInstalledDesiredVersionsLazyQuery,
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
  // status: {
  //   marginRight: theme.spacing(1)
  // },
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

interface VersionsProps {
  className: string
}

const Versions: React.FC<VersionsProps> = props => {
  const { className, ...rest } = props;
  const classes = useStyles();

  const [consumer, setConsumer] = useState<string>()
  const [developerVersions, setDeveloperVersions] = useState(new Array<DeveloperDesiredVersion>())
  const [clientVersions, setClientVersions] = useState(new Array<ClientDesiredVersion>())
  const [onlyAlerts, setOnlyAlerts] = useState(false)

  React.useEffect(() => {
    setClientVersions(new Array<ClientDesiredVersion>())
    getVersions(consumer)
  }, [consumer]);

  const consumersInfo = useDistributionConsumersInfoQuery()

  const [ getDeveloperDesiredVersions, developerDesiredVersions ] = useDeveloperDesiredVersionsLazyQuery()
  if (developerDesiredVersions.data) {
    setDeveloperVersions(developerDesiredVersions.data.developerDesiredVersions)
  }

  const [getClientDesiredVersions, clientDesiredVersions] = useClientDesiredVersionsLazyQuery()
  if (!consumer && clientDesiredVersions.data) {
    setClientVersions(clientDesiredVersions.data.clientDesiredVersions)
  }

  const [getInstalledDesiredVersions, installedDesiredVersions] = useInstalledDesiredVersionsLazyQuery()
  if (consumer && installedDesiredVersions.data) {
    setClientVersions(installedDesiredVersions.data.installedDesiredVersions)
  }

  const [getServiceStates, serviceStates] = useServiceStatesLazyQuery()

  const getVersions = (consumer:string|undefined) => {
    getDeveloperDesiredVersions()
    if (consumer) {
      getInstalledDesiredVersions({ variables: { distribution: consumer} })
      getServiceStates({ variables: { distribution: consumer} })
    } else {
      getClientDesiredVersions()
      getServiceStates({ variables: { distribution: Utils.getDistributionName() } })
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
                onChange={(event) => setConsumer(event.target.value as string)}
                title='Select consumer'
                value={consumer}
              >
                { consumersInfo.data?.distributionConsumersInfo.map(consumer =>
                    <option key={consumer.distributionName}>{consumer.distributionName}</option>) }
              </Select>}
              label='Consumer'
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
              label={null}
              control={<Button
                onClick={() => getVersions(consumer)}
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
            distributionName={consumer?consumer:Utils.getDistributionName()}
            clientVersions={clientVersions}
            developerVersions={developerVersions}
            serviceStates={serviceStates.data?.serviceStates.map(state => state.instance)}
            onlyAlerts={onlyAlerts}
          />
        </div>
      </CardContent>
    </Card>
  )
}

export default Versions;