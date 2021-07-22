import React, {useCallback, useState} from 'react';
import clsx from 'clsx';
import { makeStyles } from '@material-ui/styles';
import {
  Button,
  Card,
  CardContent, CardHeader, Select,
} from '@material-ui/core';
import {
  useClientVersionsInfoQuery,
  useDeveloperServicesQuery,
  useDeveloperVersionsInfoQuery,
  useDeveloperVersionsInProcessQuery, useProviderDesiredVersionsLazyQuery,
  useProviderDesiredVersionsQuery,
  useProvidersInfoQuery
} from "../../../../generated/graphql";
import GridTable from "../../../../common/components/gridTable/GridTable";
import {Version} from "../../../../common";
import Alert from "@material-ui/lab/Alert";
import FormGroup from "@material-ui/core/FormGroup";
import {RefreshControl} from "../../../../common/components/refreshControl/RefreshControl";
import {useHistory} from "react-router-dom";
import BuildIcon from "@material-ui/icons/Build";
import VisibilityIcon from "@material-ui/icons/Visibility";
import {GridTableColumnParams, GridTableColumnValue} from "../../../../common/components/gridTable/GridTableColumn";
import FormControlLabel from "@material-ui/core/FormControlLabel";
import Checkbox from "@material-ui/core/Checkbox";

const useStyles = makeStyles((theme:any) => ({
  root: {},
  content: {
    padding: 0
  },
  inner: {
    minWidth: 800
  },
  providerSelect: {
    width: '150px',
    paddingRight: '2px'
  },
  serviceSelect: {
    width: '150px',
    paddingRight: '2px'
  },
  downloadUpdates: {
    paddingRight: '2px'
  },
  rebuildWithNewConfig: {
    paddingRight: '2px'
  },
  versionsTable: {
    marginTop: 20
  },
  serviceColumn: {
    width: '150px',
    padding: '4px',
    paddingLeft: '16px'
  },
  versionColumn: {
    width: '100px',
    padding: '4px',
    paddingLeft: '16px'
  },
  authorColumn: {
    width: '150px',
    padding: '4px',
    paddingLeft: '16px'
  },
  commentColumn: {
    padding: '4px',
    paddingLeft: '16px'
  },
  statusColumn: {
    width: '100px',
    padding: '4px',
    paddingLeft: '16px'
  },
  timeColumn: {
    width: '200px',
    padding: '4px',
    paddingLeft: '16px'
  },
  actionsColumn: {
    width: '120px',
    padding: '4px',
    paddingRight: '30px',
    textAlign: 'right'
  },
  control: {
    paddingLeft: '10px',
    textTransform: 'none'
  },
  alert: {
    marginTop: 25
  }
}));

const BuildClient = () => {
  const classes = useStyles()

  const [provider, setProvider] = useState<string>()
  const [service, setService] = useState<string>()
  const [downloadUpdates, setDownloadUpdates] = useState<boolean>(true)
  const [rebuildWithNewConfig, setRebuildWithNewConfig] = useState<boolean>(false)
  const [error, setError] = useState<string>()

  const { data: providers } = useProvidersInfoQuery({
    fetchPolicy: 'no-cache',
    onError(err) { setError('Query providers info error ' + err.message) },
    onCompleted() { setError(undefined) }
  })
  const [ getProviderDesiredVersions, providerDesiredVersions ] = useProviderDesiredVersionsLazyQuery({
    fetchPolicy: 'no-cache',
    onError(err) { setError('Query provider desired versions error ' + err.message) },
    onCompleted() { setError(undefined) }
  })
  const { data: developerVersions, refetch: getDeveloperVersions } = useDeveloperVersionsInfoQuery({
    fetchPolicy: 'no-cache',
    onError(err) { setError('Query developer versions error ' + err.message) },
    onCompleted() { setError(undefined) }
  })
  const { data: clientVersions, refetch: getClientVersions } = useClientVersionsInfoQuery({
    fetchPolicy: 'no-cache',
    onError(err) { setError('Query client versions error ' + err.message) },
    onCompleted() { setError(undefined) }
  })

  React.useEffect(() => {
    if (provider) {
      getProviderDesiredVersions({ variables: { distribution: provider } })
    }
  }, [ 'provider' ])

  const makeServicesList = () => {
    const servicesSet = new Set<string>()

    if (providerDesiredVersions.data) {
      providerDesiredVersions.data.providerDesiredVersions.forEach(
        version => servicesSet.add(version.service)
      )
    }

    if (developerVersions?.developerVersionsInfo) {
      developerVersions.developerVersionsInfo.forEach(
        version => servicesSet.add(version.service)
      )
    }

    if (clientVersions?.clientVersionsInfo) {
      clientVersions.clientVersionsInfo.forEach(
        version => servicesSet.add(version.service)
      )
    }

    return Array.from(servicesSet)
  }

  const services = makeServicesList()

  const columns: Array<GridTableColumnParams> = [
    {
      name: 'service',
      headerName: 'Service',
      className: classes.serviceColumn,
    },
    {
      name: 'providerVersion',
      headerName: 'Provider Version',
      className: classes.versionColumn,
    },
    {
      name: 'developerVersion',
      headerName: 'Developer Version',
      className: classes.versionColumn,
    },
    {
      name: 'clientVersion',
      headerName: 'Client Version',
      className: classes.versionColumn,
    }
  ]

  const rows = services.filter(s => { return service == undefined || s == service }).sort().map(
    service => {
      const providerVersion = providerDesiredVersions.data?.providerDesiredVersions.find(version => version.service == service)
      const developerVersion = developerVersions?.developerVersionsInfo.find(version => version.service == service)
      const clientVersion = clientVersions?.clientVersionsInfo.find(version => version.service == service)
      return new Map<string, GridTableColumnValue>([
        ['service', service],
        ['providerVersion', providerVersion?Version.developerDistributionVersionToString(providerVersion.version):''],
        ['developerVersion', developerVersion?Version.developerDistributionVersionToString(developerVersion.version):''],
        ['clientVersion', clientVersion?Version.clientDistributionVersionToString(clientVersion.version):'']
      ])
    })

  return (
    <Card
      className={clsx(classes.root)}
    >
      <CardHeader
        action={
          <FormGroup row>
            { providers?.providersInfo.length ?
              <FormControlLabel
                className={classes.control}
                control={
                <Select
                  className={classes.providerSelect}
                  native
                  onChange={(event) => setProvider(event.target.value as string)}
                  title='Select provider'
                  value={provider}
                >
                  { providers?.providersInfo.map(provider =>
                    <option key={provider.distribution}>{provider.distribution}</option>) }
                </Select>}
                label='Provider'
              /> : null }
            <FormControlLabel
              className={classes.control}
              control={
              <Select
                className={classes.serviceSelect}
                native
                onChange={(event) => {
                  const choice = event.target.value as string
                  setService(choice != 'All Services' ? choice : undefined)
                }}
                title='Select service'
                value={provider}
              >
              { (new Array('All Services').concat(services))
                  .map((service, index) => <option key={index}>{service}</option>) }
            </Select>}
            label='Service'
            />
            { provider ?
            <FormControlLabel
              className={classes.control}
              control={<Checkbox
                checked={downloadUpdates}
                className={classes.downloadUpdates}
                onChange={event => setDownloadUpdates(event.target.checked)}
              />}
              label='Download Updates'
            /> : null }
            <FormControlLabel
              className={classes.control}
              control={<Checkbox
                checked={rebuildWithNewConfig}
                className={classes.rebuildWithNewConfig}
                onChange={event => setRebuildWithNewConfig(event.target.checked)}
              />}
              label='Rebuild With New Config'
            />
            <RefreshControl
              className={classes.control}
              refresh={ () => {
                if (provider) {
                  getProviderDesiredVersions({ variables: { distribution: provider } })
                }
                getDeveloperVersions()
                getClientVersions() }}
            />
          </FormGroup>
        }
        title='Build Client Service Version'
      />
      <CardContent className={classes.content}>
        <div className={classes.inner}>
          <GridTable
           className={classes.versionsTable}
           columns={columns}
           rows={rows?rows:[]}
           // onClick={(row, values) => handleOnClick(values.get('service')! as string)}
          />
          {error && <Alert className={classes.alert} severity="error">{error}</Alert>}
        </div>
      </CardContent>
    </Card>
  );
}

export default BuildClient