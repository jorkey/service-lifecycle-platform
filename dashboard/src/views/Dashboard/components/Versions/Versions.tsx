import React, {useState} from 'react';
import clsx from 'clsx';
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
import {VersionsTable} from './VersionsTable';
import {
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
  formControl: {
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
}

const Versions: React.FC<VersionsProps> = props => {
  const classes = useStyles();

  const [consumer, setConsumer] = useState<string>()
  const [onlyAlerts, setOnlyAlerts] = useState(false)

  React.useEffect(() => {
      getVersions(consumer)
  }, [consumer]);

  const consumersInfo = useDistributionConsumersInfoQuery()
  const [getDeveloperDesiredVersions, developerDesiredVersions ] = useDeveloperDesiredVersionsLazyQuery()
  const [getClientDesiredVersions, clientDesiredVersions] = useClientDesiredVersionsLazyQuery()
  const [getInstalledDesiredVersions, installedDesiredVersions] = useInstalledDesiredVersionsLazyQuery()
  const [getServiceStates, serviceStates] = useServiceStatesLazyQuery()

  const getVersions = (consumer:string|undefined) => {
    getDeveloperDesiredVersions()
    if (consumer) {
      getInstalledDesiredVersions({ variables: { distribution: consumer} })
      getServiceStates({ variables: { distribution: consumer} })
    } else {
      getClientDesiredVersions()
      getServiceStates({ variables: { distribution: localStorage.getItem('distribution')! } })
    }
  }

  return (
    <Card
      {...props}
      className={clsx(classes.root)}
    >
      <CardHeader
        action={
          <FormGroup row>
            <FormControlLabel
              className={classes.formControl}
              control={<Select
                className={classes.distributionSelect}
                native
                onChange={(event) => setConsumer(event.target.value as string)}
                title='Select consumer'
                value={consumer}
              >
                { consumersInfo.data?.distributionConsumersInfo.map(consumer =>
                    <option key={consumer.distribution}>{consumer.distribution}</option>) }
              </Select>}
              label='Consumer'
            />
            <FormControlLabel
              className={classes.formControl}
              control={<Checkbox
                checked={onlyAlerts}
                className={classes.onlyAlerts}
                onChange={event => setOnlyAlerts(event.target.checked)}
              />}
              label='Only Alerts'
            />
            <FormControlLabel
              className={classes.formControl}
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
          { developerDesiredVersions.data ?
            <VersionsTable
              developerVersions={developerDesiredVersions.data.developerDesiredVersions}
              clientVersions={consumer ? installedDesiredVersions.data?.installedDesiredVersions : clientDesiredVersions.data?.clientDesiredVersions}
              serviceStates={serviceStates.data?.serviceStates.map(state => state.instance)}
              onlyAlerts={onlyAlerts}
            />
          : null }
        </div>
      </CardContent>
    </Card>
  )
}

export default Versions;