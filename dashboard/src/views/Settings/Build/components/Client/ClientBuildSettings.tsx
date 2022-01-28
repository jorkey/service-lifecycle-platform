import React, {useState} from 'react';
import {RouteComponentProps} from 'react-router-dom'

import {
  useBuildClientServiceConfigLazyQuery,
  useBuildClientServicesQuery,
  useProfileServicesLazyQuery, useProviderDesiredVersionsLazyQuery,
  useProvidersInfoLazyQuery,
  useProvidersInfoQuery,
  useSetBuildClientServiceConfigMutation,
} from "../../../../../generated/graphql";
import BuildSettings from "../BuildSettings";

interface ClientServiceRouteParams {
  service?: string
}

interface ClientServiceEditorParams extends RouteComponentProps<ClientServiceRouteParams> {
  new?: boolean
  fromUrl: string
}

const ClientBuildSettings: React.FC<ClientServiceEditorParams> = props => {
  const service = props.match.params.service?props.match.params.service:props.new?undefined:''

  const [services, setServices] = useState(new Set<string>())
  const [error, setError] = useState<string>()

  const { data: buildServices } = useBuildClientServicesQuery({
    fetchPolicy: 'no-cache', // base option no-cache does not work
    onError(err) {
      setError('Query client services error ' + err.message)
    }
  })

  const [ getBuildServiceConfig, buildServiceConfig ] = useBuildClientServiceConfigLazyQuery({
    fetchPolicy: 'no-cache', // base option no-cache does not work
    onError(err) {
      setError('Get service config error ' + err.message)
    }
  })

  const [ getSelfServicesProfile, selfServicesProfile ] = useProfileServicesLazyQuery({
    fetchPolicy: 'no-cache', // base option no-cache does not work
    variables: { profile: 'self' },
    onCompleted(profile) {
      profile.serviceProfiles.forEach(profile => {
        return profile.services.forEach(service =>
          setServices(services => new Set(services.add(service))))})
    },
    onError(err) { setError('Query self profile services error ' + err.message) },
  })

  const [ getProviders ] = useProvidersInfoLazyQuery({
    fetchPolicy: 'no-cache', // base option no-cache does not work
    onCompleted(providers) {
      providers.providersInfo.forEach(provider =>
        getProviderDesiredVersions({ variables: { distribution: provider.distribution } }))
    },
    onError(err) { setError('Query providers info error ' + err.message) },
  })

  const [ getProviderDesiredVersions ] = useProviderDesiredVersionsLazyQuery({
    fetchPolicy: 'no-cache', // base option no-cache does not work
    onCompleted(versions) {
      versions.providerDesiredVersions.forEach(version =>
        setServices(services => new Set(services.add(version.service))))
    },
    onError(err) { setError('Query provider desired versions error ' + err.message) },
  })

  const [ setBuildServiceConfig ] =
    useSetBuildClientServiceConfigMutation({
      onError(err) { setError('Set client service config error ' + err.message) },
    })

  if (props.new) {
    if (!selfServicesProfile.data && !selfServicesProfile.loading) {
      getSelfServicesProfile()
      getProviders()
    }
  } else if (!buildServiceConfig.data && !buildServiceConfig.loading) {
    getBuildServiceConfig({variables: {service: service!}})
  }

  if (buildServices?.buildClientServicesConfig &&
      (props.new || buildServiceConfig.data?.buildClientServicesConfig)) {

    const config = buildServiceConfig.data?.buildClientServicesConfig.length?
      buildServiceConfig.data.buildClientServicesConfig[0]:undefined

    const distribution = config?.distribution
    const environment = config?config.environment:[]
    const repositories = config?config.repositories:[]
    const privateFiles = config?config.privateFiles:[]
    const macroValues = config?config.macroValues:[]

    return (<BuildSettings
              service={service}
              services={Array.from(services)}
              distribution={distribution}
              environment={environment}
              repositoriesTitle={`Config Repositories`}
              repositories={repositories}
              privateFiles={privateFiles}
              uploadPrivateFilePath={'/load/client-private-file'}
              macroValues={macroValues}
              hasService={(service =>
                !!buildServices?.buildClientServicesConfig.find(s => s.service == service))}
              validate={(environment, repositories, macroValues) => true }
              setServiceConfig={(service, distribution, environment, repositories, privateFiles, macroValues) =>
                setBuildServiceConfig({ variables: { service, distribution, environment, repositories, privateFiles, macroValues } })
                  .then((r) => !!r.data?.setBuildClientServiceConfig)
              }
              error={error}
              fromUrl={props.fromUrl}
      />)
  } else {
    return null
  }
}

export default ClientBuildSettings;
