import React, {useState} from 'react';
import clsx from 'clsx';
import { makeStyles } from '@material-ui/styles';
import {
  Box,
  Card,
  CardContent, CardHeader, Select,
} from '@material-ui/core';
import {
  ClientDistributionVersion, ClientVersionInfo,
  DeveloperDesiredVersionInput,
  DeveloperDistributionVersion,
  DistributionProviderInfo,
  useBuildClientVersionsMutation,
  useClientVersionsInfoQuery,
  useDeveloperVersionsInfoQuery,
  useProviderDesiredVersionsLazyQuery,
  useProvidersInfoQuery, useProviderTestedVersionsLazyQuery,
  useSetProviderTestedVersionsMutation, useSetTestedVersionsMutation, useTestedVersionsLazyQuery,
} from "../../../../generated/graphql";
import GridTable from "../../../../common/components/gridTable/GridTable";
import {Version} from "../../../../common";
import Alert from "@material-ui/lab/Alert";
import FormGroup from "@material-ui/core/FormGroup";
import {RefreshControl} from "../../../../common/components/refreshControl/RefreshControl";
import {GridTableColumnParams, GridTableColumnValue} from "../../../../common/components/gridTable/GridTableColumn";
import FormControlLabel from "@material-ui/core/FormControlLabel";
import Button from "@material-ui/core/Button";
import {RouteComponentProps, useHistory} from "react-router-dom";

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
    marginTop: '25px',
    paddingRight: '25px',
    display: 'flex',
    justifyContent: 'flex-end',
    p: 2
  },
  control: {
    marginLeft: '20px',
    paddingLeft: '10px',
    textTransform: 'none'
  },
  alert: {
    marginTop: 25
  }
}));

interface RowData {
  selected: boolean
  service: string
  providerVersion?: DeveloperDistributionVersion
  developerVersion?: DeveloperDistributionVersion
  clientVersion?: ClientDistributionVersion
  testedVersion?: DeveloperDistributionVersion
}

interface BuildRouteParams {
}

interface BuildServiceParams extends RouteComponentProps<BuildRouteParams> {
  fromUrl: string
}

const StartBuildClientServices: React.FC<BuildServiceParams> = props => {
  const classes = useStyles()

  const [provider, setProvider] = useState<DistributionProviderInfo>()
  const [error, setError] = useState<string>()
  const [rows, setRows] = useState<RowData[]>([])

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
  const [ getProviderVersions, providerVersions ] = useProviderDesiredVersionsLazyQuery({
    fetchPolicy: 'no-cache',
    onError(err) { setError('Query provider desired versions error ' + err.message) },
  })
  const { data: developerVersions, refetch: getDeveloperVersions } = useDeveloperVersionsInfoQuery({
    fetchPolicy: 'no-cache',
    onError(err) { setError('Query developer versions error ' + err.message) },
  })
  const { data: clientVersions, refetch: getClientVersions } = useClientVersionsInfoQuery({
    fetchPolicy: 'no-cache',
    onError(err) { setError('Query client versions error ' + err.message) },
    onCompleted() { setError(undefined) }
  })
  const [ getTestedVersions, testedVersions ] = useTestedVersionsLazyQuery({
    fetchPolicy: 'no-cache',
    onError(err) { setError('Query tested versions error ' + err.message) },
  })
  const [ getProviderTestedVersions, providerTestedVersions ] = useProviderTestedVersionsLazyQuery({
    fetchPolicy: 'no-cache',
    onError(err) { setError('Query provider tested versions error ' + err.message) },
  })
  const [ buildClientVersions ] = useBuildClientVersionsMutation({
    variables: {
      versions: rows.filter(row => row.selected && (row.providerVersion || row.developerVersion)).map(row => {
        const version = row.providerVersion?row.providerVersion:row.developerVersion!
        return { service: row.service, version: {
          distribution: version.distribution,
          build: version.build
        } } as DeveloperDesiredVersionInput })
    },
    fetchPolicy: 'no-cache',
    onError(err) {
      setError('Build version error ' + err.message)
    },
    onCompleted() {
      history.push(props.fromUrl + '/monitor')
    }
  })

  const [ setTestedVersions ] = useSetTestedVersionsMutation({
    onCompleted() {
      getTestedVersions()
    }
  })
  const [ setProviderTestedVersions ] = useSetProviderTestedVersionsMutation({
    onCompleted() {
      getProviderTestedVersions({ variables: { distribution: provider!.distribution } })
    }
  })

  const history = useHistory()

  React.useEffect(() => {
    if (provider) {
      getProviderVersions({ variables: { distribution: provider.distribution } })
      getProviderTestedVersions({ variables: { distribution: provider.distribution } })
    } else {
      getTestedVersions()
    }
  }, [ provider ])

  React.useEffect(() => {
    if (provider) {
      getProviderTestedVersions({ variables: {distribution: provider.distribution} })
    }
  }, [ providerVersions ])

  React.useEffect(() => {
    setRows(makeRowsData())
  }, [ provider, providerVersions, developerVersions, clientVersions, testedVersions, providerTestedVersions ])

  const makeServicesList = () => {
    const servicesSet = new Set<string>()

    if (provider) {
      if (providerVersions.data) {
        providerVersions.data.providerDesiredVersions.forEach(
          version => servicesSet.add(version.service)
        )
      }
    } else if (developerVersions?.developerVersionsInfo) {
      developerVersions.developerVersionsInfo.forEach(
        version => {
          if (version.version.distribution == localStorage.getItem('distribution')) {
            servicesSet.add(version.service)
          }
        }
      )
    }

    return Array.from(servicesSet)
  }

  const makeRowsData: () => RowData[] = () => {
    const services = makeServicesList()
    return services.sort().map(
      service => {
        let selected = false
        const providerVersion = providerVersions.data?.providerDesiredVersions
          .find(version => version.service == service)
        const developerVersion = developerVersions?.developerVersionsInfo
          .sort((v1, v2) => Version.compareDeveloperDistributionVersions(v1.version, v2.version))
          .reverse()
          .find(version => version.service == service)
        const clientVersion = clientVersions?.clientVersionsInfo
          .sort((v1, v2) => Version.compareClientDistributionVersions(v1.version, v2.version))
          .reverse()
          .find(version => version.service == service)
        const testedVersion = testedVersions?.data?.testedVersions?.find(version => version.service == service)
        const providerTestedVersion = providerTestedVersions?.data?.providerTestedVersions
          .find(version => version.service == service)

        if (providerVersion && Version.compareDeveloperDistributionVersions(developerVersion?.version, providerVersion?.version) != 0) {
          selected = true
        }
        if (Version.compareBuilds(developerVersion?.version.build, clientVersion?.version.developerBuild) != 0) {
          selected = true
        }
        return {
          selected: selected,
          service: service,
          providerVersion: providerVersion?.version,
          developerVersion: developerVersion?.version,
          clientVersion: clientVersion?.version,
          testedVersion: testedVersion?testedVersion?.version:providerTestedVersion?.version
        } as RowData
      })
  }

  const validate = () => !!rowsView.find(value => value.get("selected"))

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
    },
    {
      name: 'testedVersion',
      headerName: 'Tested Developer Version',
      className: classes.versionColumn,
    }
  ].filter(column => column.name != 'providerVersion' || !!provider)
   .filter(column => column.name != 'testedVersion' || !!providerTestedVersions.data || providerTestedVersions.loading)

  const rowsView = rows.map(row =>
    new Map<string, GridTableColumnValue>([
      ['selected', row.selected],
      ['service', row.service],
      ['providerVersion', row.providerVersion?Version.developerDistributionVersionToString(row.providerVersion):''],
      ['developerVersion', row.developerVersion?Version.developerDistributionVersionToString(row.developerVersion):''],
      ['clientVersion', row.clientVersion?Version.clientDistributionVersionToString(row.clientVersion):''],
      ['testedVersion', row.testedVersion?Version.developerDistributionVersionToString(row.testedVersion):'']
    ])
  )

  function clientVersionToDeveloperVersion(version: ClientVersionInfo): DeveloperDesiredVersionInput {
    return {
      service: version.service,
      version: { distribution: version.version.distribution, build: version.version.developerBuild }
    }
  }

  function isTested() {
    const installedVersions =
      (provider?
          clientVersions?.clientVersionsInfo?.filter(version => version.version.distribution == provider.distribution):
          clientVersions?.clientVersionsInfo?.filter(version => version.version.distribution == localStorage.getItem('distribution')))
      ?.map(version => clientVersionToDeveloperVersion(version))
    if (installedVersions) {
      const markedAsTested = provider?providerTestedVersions?.data?.providerTestedVersions:testedVersions?.data?.testedVersions
      return !installedVersions.find(version => {
        const testedVersion = markedAsTested?.find(v => {
          return version.service == v.service
        })
        if (testedVersion) {
          return Version.compareDeveloperDistributionVersions(version.version, testedVersion.version) != 0
        } else {
          return true
        }
      })
    } else {
      return false
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
                      const distribution = providers?.providersInfo
                        .find(provider => provider.distribution == event.target.value as string)
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
                  getProviderVersions({ variables: { distribution: provider.distribution } })
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
             rows={rowsView?rowsView:[]}
             checkBoxColumn={true}
             disableManualCheck={provider && !!provider.testConsumer}
             onRowsChecked={(rowsNum) => {
               setRows(rows.map((row, index) => { return {
                 selected: (rowsNum.find(row => row == index) != undefined)?true:row.selected, service: row.service, providerVersion: row.providerVersion,
                 developerVersion: row.developerVersion, clientVersion: row.clientVersion, testedVersion: row.testedVersion } as RowData }))
             }}
             onRowsUnchecked={(rowsNum) => {
               setRows(rows.map((row, index) => { return {
                 selected: (rowsNum.find(row => row == index) != undefined)?false:row.selected, service: row.service, providerVersion: row.providerVersion,
                 developerVersion: row.developerVersion, clientVersion: row.clientVersion, testedVersion: row.testedVersion } as RowData }))
             }}
          />
          {error && <Alert className={classes.alert} severity="error">{error}</Alert>}
          <Box className={classes.controls}>
            <Button className={classes.control}
                    color="primary"
                    variant="contained"
                    disabled={!validate()}
                    onClick={() => buildClientVersions()}
            >
              Update Client
            </Button>
            {clientVersions?<Button className={classes.control}
                    color="primary"
                    variant="contained"
                    disabled={isTested()}
                    onClick={() => {
                      if (provider) {
                        setProviderTestedVersions({
                          variables: {
                            distribution: provider.distribution,
                            versions: clientVersions.clientVersionsInfo
                              .filter(version => version.version.distribution == provider.distribution)
                              .map(version => clientVersionToDeveloperVersion(version)
                            )
                          }
                        })
                      } else {
                        setTestedVersions({
                          variables: {
                            versions: clientVersions.clientVersionsInfo
                              .filter(version => version.version.distribution == localStorage.getItem('distribution'))
                              .map(version => clientVersionToDeveloperVersion(version))
                        }})
                      }}
                    }
            >
              Mark As Tested
            </Button>:null}
          </Box>
        </div>
      </CardContent>
    </Card>
  );
}

export default StartBuildClientServices