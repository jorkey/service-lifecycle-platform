import React, {useState} from 'react';
import {RouteComponentProps} from 'react-router-dom'

import {
  useBuildDeveloperServiceConfigLazyQuery,
  useBuildDeveloperServicesQuery,
  useSetBuildDeveloperServiceConfigMutation
} from "../../../../../generated/graphql";
import BuildSettings from "../BuildSettings";

interface DeveloperServiceRouteParams {
  service?: string
}

interface DeveloperServiceEditorParams extends RouteComponentProps<DeveloperServiceRouteParams> {
  new?: boolean
  fromUrl: string
}

const DeveloperBuildSettings: React.FC<DeveloperServiceEditorParams> = props => {
  const service = props.match.params.service?props.match.params.service:props.new?undefined:''

  const [error, setError] = useState<string>()

  const { data: buildDeveloperServicesConfig } = useBuildDeveloperServicesQuery({
    fetchPolicy: 'no-cache', // base option no-cache does not work
    onError(err) {
      setError('Query developer services error ' + err.message)
    }
  })

  const [ getBuildServiceConfig, buildServiceConfig ] = useBuildDeveloperServiceConfigLazyQuery({
    fetchPolicy: 'no-cache', // base option no-cache does not work
    onError(err) {
      setError('Get service config error ' + err.message)
    }
  })

  const [ setBuildServiceConfig ] =
    useSetBuildDeveloperServiceConfigMutation({
      onError(err) { setError('Set developer service config error: ' + err.message) },
    })

  if (!props.new && !buildServiceConfig.data && !buildServiceConfig.loading) {
    getBuildServiceConfig({variables: {service: service!}})
  }

  const config = buildServiceConfig.data?.buildDeveloperServicesConfig.length?
    buildServiceConfig.data.buildDeveloperServicesConfig[0]:undefined

  if ((props.new || config) && buildDeveloperServicesConfig?.buildDeveloperServicesConfig) {
    const distribution = config?.distribution
    const environment = config?config.environment:[]
    const repositories = config?config.repositories:[]
    const macroValues = config?config.macroValues:[]

    return (<BuildSettings
              service={service}
              distribution={distribution}
              environment={environment}
              repositoriesTitle={`Source Repositories`}
              repositories={repositories}
              macroValues={macroValues}
              hasService={(service =>
                !!buildDeveloperServicesConfig?.buildDeveloperServicesConfig.find(s => s.service == service))}
              validate={(environment, repositories, macroValues) =>
                true }
              setServiceConfig={(service, distribution, environment, repositories, macroValues) =>
                setBuildServiceConfig({ variables: { service, distribution, environment, repositories, macroValues } })
                  .then((r) => !!r.data?.setBuildDeveloperServiceConfig) }
              error={error}
              fromUrl={props.fromUrl}
      />)
  } else {
    return null
  }
}

export default DeveloperBuildSettings;
