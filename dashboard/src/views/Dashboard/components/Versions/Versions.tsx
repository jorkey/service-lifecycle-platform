import React, {useState} from 'react';
import { makeStyles } from '@material-ui/styles';
import {
  Card,
  CardHeader,
  CardContent,
  Select
} from '@material-ui/core';
import FormGroup from '@material-ui/core/FormGroup';
import FormControlLabel from '@material-ui/core/FormControlLabel';
import Checkbox from '@material-ui/core/Checkbox';
import {VersionsTable} from './VersionsTable';
import {
  useClientDesiredVersionsLazyQuery, useConsumerAccountsInfoQuery,
  useDeveloperDesiredVersionsLazyQuery,
  useInstalledDesiredVersionsLazyQuery,
  useServiceStatesLazyQuery
} from "../../../../generated/graphql";
import {RefreshControl} from "../../../../common/components/refreshControl/RefreshControl";

const useStyles = makeStyles(theme => ({
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
  actions: {
    justifyContent: 'flex-end'
  },
  control: {
    paddingLeft: '10px',
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
  const [onlyAlerts, setOnlyAlerts] = useState(true)

  React.useEffect(() => {
      getVersions(consumer)
  }, [consumer]);

  const { data: consumerAccountsInfo } = useConsumerAccountsInfoQuery({
    fetchPolicy: 'no-cache', // base option no-cache does not work
  })
  const [getDeveloperDesiredVersions, developerDesiredVersions ] = useDeveloperDesiredVersionsLazyQuery({
    fetchPolicy: 'no-cache', // base option no-cache does not work
  })
  const [getClientDesiredVersions, clientDesiredVersions] = useClientDesiredVersionsLazyQuery({
    fetchPolicy: 'no-cache', // base option no-cache does not work
  })
  const [getInstalledDesiredVersions, installedDesiredVersions] = useInstalledDesiredVersionsLazyQuery({
    fetchPolicy: 'no-cache', // base option no-cache does not work
  })
  const [getServiceStates, serviceStates] = useServiceStatesLazyQuery({
    fetchPolicy: 'no-cache', // base option no-cache does not work
  })

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
    <Card>
      <CardHeader
        action={
          <FormGroup row>
            { consumerAccountsInfo?.consumerAccountsInfo.length ?
              <FormControlLabel
                className={classes.control}
                control={<Select
                  className={classes.distributionSelect}
                  native
                  onChange={
                    (event) => setConsumer(
                      event.target.value ? event.target.value as string : undefined)
                  }
                  title='Select consumer'
                  value={consumer}
                >
                  <option key={''}></option>
                  { consumerAccountsInfo?.consumerAccountsInfo
                      .map(consumer => <option key={consumer.account}>{consumer.account}</option>) }
                </Select>}
                label='Consumer'
              /> : null }
            <FormControlLabel
              className={classes.control}
              control={<Checkbox
                className={classes.onlyAlerts}
                checked={onlyAlerts}
                onChange={event => setOnlyAlerts(event.target.checked)}
              />}
              label='Only Alerts'
            />
            <RefreshControl
              className={classes.control}
              refresh={() => getVersions(consumer)}
            />
          </FormGroup>
        }
        title={onlyAlerts ? 'Version Alerts' : 'Versions'}
      />
      <CardContent className={classes.content}>
        <div className={classes.inner}>
          { developerDesiredVersions.data ?
            <VersionsTable
              developerVersions={developerDesiredVersions.data.developerDesiredVersions}
              clientVersions={consumer ? installedDesiredVersions.data?.installedDesiredVersions : clientDesiredVersions.data?.clientDesiredVersions}
              serviceStates={serviceStates.data?.serviceStates}
              onlyAlerts={onlyAlerts}
            />
          : null }
        </div>
      </CardContent>
    </Card>
  )
}

export default Versions;