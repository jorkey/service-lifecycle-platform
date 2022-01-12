import React, {useState} from 'react';
import {RouteComponentProps} from 'react-router-dom'

import {
  useBuildClientServiceConfigLazyQuery, useBuildClientServicesQuery,
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

  const [ setBuildServiceConfig ] =
    useSetBuildClientServiceConfigMutation({
      onError(err) { setError('Set client service config error ' + err.message) },
    })

  if (!props.new && !buildServiceConfig.data && !buildServiceConfig.loading) {
    getBuildServiceConfig({variables: {service: service!}})
  }

  if (buildServices?.buildClientServicesConfig && buildServiceConfig.data?.buildClientServicesConfig) {
    const config = buildServiceConfig.data?.buildClientServicesConfig.length?
      buildServiceConfig.data.buildClientServicesConfig[0]:undefined

    const distribution = config?.distribution
    const environment = config?config.environment:[]
    const repositories = config?config.repositories:[]
    const macroValues = config?config.macroValues:[]

    return (<BuildSettings
              service={service}
              distribution={distribution}
              environment={environment}
              repositoriesTitle={`Config Repositories`}
              repositories={repositories}
              macroValues={macroValues}
              hasService={(service =>
                !!buildServices?.buildClientServicesConfig.find(s => s.service == service))}
              validate={(environment, repositories, macroValues) => true }
              setServiceConfig={(service, distribution, environment, repositories, macroValues) =>
                setBuildServiceConfig({ variables: { service, distribution, environment, repositories, macroValues } })
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
