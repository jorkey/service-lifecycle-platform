import React, {useState} from 'react';
import { makeStyles } from '@material-ui/styles';
import {
  Box,
  Card,
  CardContent, CardHeader, Select,
} from '@material-ui/core';
import {
  ClientDesiredVersion,
  ClientDistributionVersion,
  DeveloperDesiredVersionInput,
  DeveloperDistributionVersion,
  DistributionProviderInfo,
  useBuildClientVersionsMutation,
  useClientDesiredVersionsQuery,
  useClientVersionsInfoQuery,
  useDeveloperDesiredVersionsLazyQuery,
  useProfileServicesLazyQuery,
  useProfileServicesQuery,
  useProviderDesiredVersionsLazyQuery,
  useProvidersInfoQuery,
  useProviderTestedVersionsLazyQuery,
  useSetProviderTestedVersionsMutation,
  useSetTestedVersionsMutation,
  useTestedVersionsLazyQuery,
} from "../../../../generated/graphql";
import GridTable from "../../../../common/components/gridTable/GridTable";
import {Version} from "../../../../common";
import Alert from "@material-ui/lab/Alert";
import FormGroup from "@material-ui/core/FormGroup";
import {RefreshControl} from "../../../../common/components/refreshControl/RefreshControl";
import {GridTableColumnParams} from "../../../../common/components/gridTable/GridTableColumn";
import FormControlLabel from "@material-ui/core/FormControlLabel";
import Button from "@material-ui/core/Button";
import {RouteComponentProps, useHistory} from "react-router-dom";
import {GridTableCellParams} from "../../../../common/components/gridTable/GridTableCell";

const useStyles = makeStyles((theme:any) => ({
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
  },
  serviceColumn: {
  },
  versionColumn: {
  },
  controls: {
    marginRight: 16,
    display: 'flex',
    justifyContent: 'flex-end',
    p: 2
  },
  control: {
    marginLeft: '10px',
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
    fetchPolicy: 'no-cache', // base option no-cache does not work
    onError(err) { setError('Query providers info error ' + err.message) },
    onCompleted(providers) {
      if (providers.providersInfo?.length) {
        setProvider(providers.providersInfo[0])
      }
    }
  })
  const { data: clientDesiredVersions, refetch: getClientDesiredVersions } = useClientDesiredVersionsQuery({
    fetchPolicy: 'no-cache', // base option no-cache does not work
    onCompleted() { setRows(makeRowsData()) },
    onError(err) { setError('Query client desired versions error ' + err.message) },
  })
  const { data: clientVersions } = useClientVersionsInfoQuery({
    fetchPolicy: 'no-cache', // base option no-cache does not work
    onCompleted() { setRows(makeRowsData()) },
    onError(err) { setError('Query client versions error ' + err.message) }
  })
  const [ getSelfServicesProfile, selfServicesProfile ] = useProfileServicesLazyQuery({
    fetchPolicy: 'no-cache', // base option no-cache does not work
    variables: { profile: 'self' },
    onError(err) { setError('Query self profile services error ' + err.message) },
  })
  const [ getDeveloperDesiredVersions, developerDesiredVersions ] = useDeveloperDesiredVersionsLazyQuery({
    fetchPolicy: 'no-cache', // base option no-cache does not work
    onCompleted() { setRows(makeRowsData()) },
    onError(err) { setError('Query developer desired versions error ' + err.message) },
  })
  const [ getProviderDesiredVersions, providerDesiredVersions ] = useProviderDesiredVersionsLazyQuery({
    fetchPolicy: 'no-cache', // base option no-cache does not work
    onCompleted() { setRows(makeRowsData()) },
    onError(err) { setError('Query provider desired versions error ' + err.message) },
  })
  const [ getTestedVersions, testedVersions ] = useTestedVersionsLazyQuery({
    fetchPolicy: 'no-cache', // base option no-cache does not work
    onCompleted() { setRows(makeRowsData()) },
    onError(err) { setError('Query tested versions error ' + err.message) },
  })
  const [ getProviderTestedVersions, providerTestedVersions ] = useProviderTestedVersionsLazyQuery({
    fetchPolicy: 'no-cache', // base option no-cache does not work
    onCompleted() { setRows(makeRowsData()) },
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
    onError(err) {
      setError('Build version error ' + err.message)
    },
    onCompleted(data) {
      history.push(props.fromUrl + '/monitor/' + data.buildClientVersions)
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
      getProviderDesiredVersions({ variables: { distribution: provider.distribution } })
      getProviderTestedVersions({ variables: { distribution: provider.distribution } })
    } else {
      getSelfServicesProfile()
      getDeveloperDesiredVersions()
    }
    getTestedVersions()
    setRows(makeRowsData())
  }, [ provider ])

  const makeServicesList = () => {
    const servicesSet = new Set<string>()

    if (provider) {
      if (providerDesiredVersions.data) {
        providerDesiredVersions.data.providerDesiredVersions.forEach(
          version => servicesSet.add(version.service)
        )
      }
    } else if (selfServicesProfile.data) {
      selfServicesProfile.data.serviceProfiles.forEach(
        profiles => profiles.services.forEach(service => servicesSet.add(service))
      )
    }

    return Array.from(servicesSet)
  }

  const makeRowsData: () => RowData[] = () => {
    if (clientDesiredVersions && clientVersions &&
        (!provider || (providerDesiredVersions.data && testedVersions.data?.testedVersions))) {
      const services = makeServicesList()
      return services.sort().map(
        service => {
          const providerVersion = providerDesiredVersions.data?.providerDesiredVersions
            .find(version => version.service == service)?.version
          const developerVersion = developerDesiredVersions.data?.developerDesiredVersions
            .find(version => version.service == service)?.version
          const clientVersion = clientDesiredVersions.clientDesiredVersions
            .find(version => version.service == service)?.version
          const testedVersion = testedVersions.data?.testedVersions?.find(version => version.service == service)
          const providerTestedVersion = providerTestedVersions?.data?.providerTestedVersions
            .find(version => version.service == service)

          const sourceVersion = providerVersion ? providerVersion : developerVersion
          const hasClientVersion = clientVersions?.clientVersionsInfo
            .filter(info => info.service == service)
            .find(info => Version.compareDeveloperDistributionVersions(sourceVersion,
              {distribution: info.version.distribution, build: info.version.developerBuild}) == 0)
          let selected = !hasClientVersion

          return {
            selected: selected,
            service: service,
            providerVersion: providerVersion,
            developerVersion: developerVersion,
            clientVersion: clientVersion,
            testedVersion: testedVersion ? testedVersion?.version : providerTestedVersion?.version
          } as RowData
        })
    } else {
      return []
    }
  }

  const validate = () => !!rowsView.find(value => value.get('select')?.value)

  const columns: Array<GridTableColumnParams> = [
    {
      name: 'select',
      type: 'checkbox',
    },
    {
      name: 'service',
      headerName: 'Service',
      className: classes.serviceColumn,
    },
    {
      name: 'providerVersion',
      headerName: 'Provider Desired Version',
      className: classes.versionColumn,
    },
    {
      name: 'developerVersion',
      headerName: 'Developer Desired Version',
      className: classes.versionColumn,
    },
    {
      name: 'clientVersion',
      headerName: 'Client Desired Version',
      className: classes.versionColumn,
    },
    {
      name: 'testedVersion',
      headerName: 'Tested Developer Version',
      className: classes.versionColumn,
    }
  ].filter(column => column.name != 'developerVersion' || !provider)
   .filter(column => column.name != 'providerVersion' || !!provider)
   .filter(column => column.name != 'testedVersion' || !!providerTestedVersions.data || providerTestedVersions.loading) as GridTableColumnParams[]

  const rowsView = rows.map(row => {
    return new Map<string, GridTableCellParams>([
      ['select', {
        value: row.selected,
        editable: !provider?.testConsumer
      }],
      ['service', { value: row.service }],
      ['providerVersion', { value: row.providerVersion?Version.developerDistributionVersionToString(row.providerVersion):'' }],
      ['developerVersion', { value: row.developerVersion?Version.developerDistributionVersionToString(row.developerVersion):'' }],
      ['clientVersion', { value: row.clientVersion?Version.clientDistributionVersionToString(row.clientVersion):'' }],
      ['testedVersion', { value: row.testedVersion?Version.developerDistributionVersionToString(row.testedVersion):'' }]
    ])})

  function clientVersionToDeveloperVersion(info: ClientDesiredVersion): DeveloperDesiredVersionInput {
    return {
      service: info.service,
      version: { distribution: info.version.distribution, build: info.version.developerBuild }
    }
  }

  function isTested() {
    const installedVersions =
      (provider?
          clientDesiredVersions?.clientDesiredVersions?.filter(version => version.version.distribution == provider.distribution):
          clientDesiredVersions?.clientDesiredVersions?.filter(version => version.version.distribution == localStorage.getItem('distribution')))
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
    <div>
      <Card>
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
                    getProviderDesiredVersions({ variables: { distribution: provider.distribution } })
                  }
                  getDeveloperDesiredVersions()
                  getClientDesiredVersions() }}
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
               onRowsSelected={(rowsNum) => {
                 setRows(rows.map((row, index) => { return {
                   selected: (rowsNum.find(row => row == index) != undefined)?true:row.selected, service: row.service, providerVersion: row.providerVersion,
                   developerVersion: row.developerVersion, clientVersion: row.clientVersion, testedVersion: row.testedVersion
                 } as RowData }))
               }}
               onRowsUnselected={(rowsNum) => {
                 setRows(rows.map((row, index) => { return {
                   selected: (rowsNum.find(row => row == index) != undefined)?false:row.selected, service: row.service, providerVersion: row.providerVersion,
                   developerVersion: row.developerVersion, clientVersion: row.clientVersion, testedVersion: row.testedVersion
                 } as RowData }))
               }}
            />
          </div>
        </CardContent>
      </Card>
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
      {clientDesiredVersions?<Button className={classes.control}
                                     color="primary"
                                     variant="contained"
                                     disabled={isTested()}
                                     onClick={() => {
                                       if (provider) {
                                         setProviderTestedVersions({
                                           variables: {
                                             distribution: provider.distribution,
                                             versions: clientDesiredVersions?.clientDesiredVersions
                                               .filter(info => info.version.distribution == provider.distribution)
                                               .map(version => clientVersionToDeveloperVersion(version)
                                               )
                                           }
                                         })
                                       } else {
                                         setTestedVersions({
                                           variables: {
                                             versions: clientDesiredVersions?.clientDesiredVersions
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
  );
}

export default StartBuildClientServices