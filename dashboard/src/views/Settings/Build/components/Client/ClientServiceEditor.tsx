import React, {useState} from 'react';
import {RouteComponentProps} from 'react-router-dom'

import {
  useBuildClientServiceConfigLazyQuery, useBuildClientServicesQuery,
  useSetBuildClientServiceConfigMutation,
} from "../../../../../generated/graphql";
import ServiceEditor from "../ServiceEditor";

interface ClientServiceRouteParams {
  service?: string
}

interface ClientServiceEditorParams extends RouteComponentProps<ClientServiceRouteParams> {
  fromUrl: string
}

const ClientServiceEditor: React.FC<ClientServiceEditorParams> = props => {
  const service = props.match.params.service

  const [error, setError] = useState<string>()

  const { data: buildClientServices } = useBuildClientServicesQuery({
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

  if (service && !buildServiceConfig.data && !buildServiceConfig.loading) {
    getBuildServiceConfig({variables: {service: service}})
  }

  const config = buildServiceConfig.data?.buildClientServicesConfig.length?
    buildServiceConfig.data.buildClientServicesConfig[0]:undefined

  if ((!service || config) && buildClientServices?.buildClientServicesConfig) {
    const environment = config?config.environment:[]
    const repositories = config?config.repositories:[]
    const macroValues = config?config.macroValues:[]

    return (<ServiceEditor
              service={service}
              environment={environment}
              repositoriesTitle={`Config Repositories`}
              repositories={repositories}
              macroValues={macroValues}
              hasService={(service =>
                !!buildClientServices?.buildClientServicesConfig.find(s => s.service == service))}
              validate={(environment, repositories, macroValues) =>
                repositories.length > 0 }
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

export default ClientServiceEditor;
