import React, {useCallback, useState} from 'react';
import clsx from 'clsx';
import { makeStyles } from '@material-ui/styles';
import {
  Box,
  Card,
  CardContent, CardHeader, Select,
} from '@material-ui/core';
import {
  DistributionProviderInfo, useBuildClientVersionsMutation,
  useClientVersionsInfoQuery, useClientVersionsInProcessQuery,
  useDeveloperVersionsInfoQuery,
  useProviderDesiredVersionsLazyQuery,
  useProvidersInfoQuery
} from "../../../../generated/graphql";
import GridTable from "../../../../common/components/gridTable/GridTable";
import {Version} from "../../../../common";
import Alert from "@material-ui/lab/Alert";
import FormGroup from "@material-ui/core/FormGroup";
import {RefreshControl} from "../../../../common/components/refreshControl/RefreshControl";
import {GridTableColumnParams, GridTableColumnValue} from "../../../../common/components/gridTable/GridTableColumn";
import FormControlLabel from "@material-ui/core/FormControlLabel";
import Button from "@material-ui/core/Button";

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
  versionsTable: {
    marginTop: '20px'
  },
  serviceColumn: {
    padding: 'none',
    paddingLeft: '16px'
  },
  versionColumn: {
    padding: 'normal',
    paddingLeft: '16px'
  },
  controls: {
    marginTop: 25,
    display: 'flex',
    justifyContent: 'flex-end',
    p: 2
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

  const [provider, setProvider] = useState<DistributionProviderInfo>()
  const [selectedRows, setSelectedRows] = useState(new Set<number>())

  const [error, setError] = useState<string>()

  const { data: providers } = useProvidersInfoQuery({
    fetchPolicy: 'no-cache',
    onError(err) { setError('Query providers info error ' + err.message) },
    onCompleted() {
      setError(undefined)
      if (providers?.providersInfo?.length) {
        setProvider(providers.providersInfo[0])
      }
    }
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

  const [ buildClientVersions ] = useBuildClientVersionsMutation({
      fetchPolicy: 'no-cache'
  })

  React.useEffect(() => {
    setSelectedRows(new Set())
    if (provider) {
      getProviderDesiredVersions({ variables: { distribution: provider.distribution } })
    }
  }, [ provider ])

  const makeServicesList = () => {
    const servicesSet = new Set<string>()

    if (provider && providerDesiredVersions.data) {
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

  const validate = () => {
    return selectedRows.size
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
  ].filter(c => provider || c.name != 'providerVersion')

  const rows = services.sort().map(
    service => {
      const providerVersion = providerDesiredVersions.data?.providerDesiredVersions
        .find(version => version.service == service)
      const developerVersion = developerVersions?.developerVersionsInfo
        .sort((v1, v2) => Version.compareDeveloperDistributionVersions(v1.version, v2.version))
        .reverse()
        .find(version => version.service == service)
      const clientVersion = clientVersions?.clientVersionsInfo
        .sort((v1, v2) => Version.compareClientDistributionVersions(v1.version, v2.version))
        .reverse()
        .find(version => version.service == service)
      return new Map<string, GridTableColumnValue>([
        ['service', service],
        ['providerVersion', providerVersion?Version.developerDistributionVersionToString(providerVersion.version):''],
        ['developerVersion', developerVersion?Version.developerDistributionVersionToString(developerVersion.version):''],
        ['clientVersion', clientVersion?Version.clientDistributionVersionToString(clientVersion.version):'']
      ])
    })

  const allSelected = provider && !!provider.testConsumer

  if (allSelected) {
    if (selectedRows.size != rows.length) {
      setSelectedRows(new Set(rows.map((value, index) => index)))
    }
  }

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
                    onChange={(event) => {
                      const distribution = providers?.providersInfo.find(provider => provider.distribution == event.target.value as string)
                      setProvider(distribution)
                    }}
                    title='Select provider'
                    value={provider?.distribution}
                  >
                    <option key={-1}/>
                    { providers?.providersInfo
                        .map((provider) => provider.distribution)
                        .map((provider, index) => <option key={index}>{provider}</option>)}
                  </Select>
                }
                label='Update From Provider'
              /> : null }
            <RefreshControl
              className={classes.control}
              refresh={ () => {
                if (provider) {
                  getProviderDesiredVersions({ variables: { distribution: provider.distribution } })
                }
                getDeveloperVersions()
                getClientVersions() }}
            />
          </FormGroup>
        }
        title={provider?'Update Client Services':'Build Client Services'}
      />
      <CardContent className={classes.content}>
        <div className={classes.inner}>
          <GridTable
             className={classes.versionsTable}
             columns={columns}
             rows={rows?rows:[]}
             selectColumn={true}
             disableManualSelect={allSelected}
             selectedRows={selectedRows}
             onRowSelected={(row, columns) => {
               setSelectedRows(new Set(selectedRows.add(row)))
             }}
             onRowUnselected={(row, columns) => {
               selectedRows.delete(row)
               setSelectedRows(new Set(selectedRows))
             }}
          />
          {error && <Alert className={classes.alert} severity="error">{error}</Alert>}
          <Box className={classes.controls}>
            <Button className={classes.control}
                    color="primary"
                    variant="contained"
                    disabled={!validate()}
                    // onClick={() => buildDeveloperVersion()}
            >
              Update Client
            </Button>
          </Box>
        </div>
      </CardContent>
    </Card>
  );
}

export default BuildClient