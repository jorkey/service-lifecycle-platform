import React, {useState} from 'react';
import {RouteComponentProps} from 'react-router-dom'

import {
  useBuildDeveloperServiceConfigLazyQuery,
  useBuildDeveloperServicesQuery,
  useSetBuildDeveloperServiceConfigMutation
} from "../../../../../generated/graphql";
import ServiceEditor from "../ServiceEditor";

interface DeveloperServiceRouteParams {
  service?: string
}

interface DeveloperServiceEditorParams extends RouteComponentProps<DeveloperServiceRouteParams> {
  fromUrl: string
}

const DeveloperServiceEditor: React.FC<DeveloperServiceEditorParams> = props => {
  const service = props.match.params.service

  const [error, setError] = useState<string>()

  const { data: builDeveloperServicesConfig } = useBuildDeveloperServicesQuery({
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

  if (service && !buildServiceConfig.data && !buildServiceConfig.loading) {
    getBuildServiceConfig({variables: {service: service}})
  }

  const config = buildServiceConfig.data?.buildDeveloperServicesConfig.length?
    buildServiceConfig.data.buildDeveloperServicesConfig[0]:undefined

  if ((!service || config) && builDeveloperServicesConfig?.buildDeveloperServicesConfig) {
    const environment = config?config.environment:[]
    const repositories = config?config.repositories:[]
    const macroValues = config?config.macroValues:[]

    return (<ServiceEditor
              service={service}
              environment={environment}
              repositoriesTitle={`Source Repositories`}
              repositories={repositories}
              macroValues={macroValues}
              hasService={(service =>
                !!builDeveloperServicesConfig?.buildDeveloperServicesConfig.find(s => s.service == service))}
              validate={(environment, repositories, macroValues) =>
                repositories.length > 0 }
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

export default DeveloperServiceEditor;
