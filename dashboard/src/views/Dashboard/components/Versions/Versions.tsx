import React, {useState} from 'react';
import clsx from 'clsx';
import { makeStyles } from '@material-ui/styles';
import {
  Card,
  CardHeader,
  CardContent,
  Divider,
  Select
} from '@material-ui/core';
import FormGroup from '@material-ui/core/FormGroup';
import FormControlLabel from '@material-ui/core/FormControlLabel';
import Checkbox from '@material-ui/core/Checkbox';
import {VersionsTable} from './VersionsTable';
import {
  AccountRole,
  useClientDesiredVersionsLazyQuery, useConsumerAccountInfoQuery,
  useDeveloperDesiredVersionsLazyQuery,
  useInstalledDesiredVersionsLazyQuery,
  useServiceStatesLazyQuery
} from "../../../../generated/graphql";
import {RefreshControl} from "../../../../common/components/refreshControl/RefreshControl";

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
  control: {
    paddingLeft: '10px',
    textTransform: 'none'
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

  const { data: consumerAccountsInfo } = useConsumerAccountInfoQuery({
    fetchPolicy: 'no-cache',
  })
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
              className={classes.control}
              control={<Select
                className={classes.distributionSelect}
                native
                onChange={(event) => setConsumer(event.target.value as string)}
                title='Select consumer'
                value={consumer}
              >
                { consumerAccountsInfo?.consumerAccountsInfo
                    .map(consumer => <option key={consumer.account}>{consumer.account}</option>) }
              </Select>}
              label='Consumer'
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
            <RefreshControl
              className={classes.control}
              refresh={() => getVersions(consumer)}
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