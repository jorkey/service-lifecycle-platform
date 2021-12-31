import React, {useState} from 'react';
import {RouteComponentProps} from 'react-router-dom'

import {
  useDeveloperServiceConfigLazyQuery,
  useDeveloperServicesQuery,
  useSetDeveloperServiceConfigMutation
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

  const { data: developerServices } = useDeveloperServicesQuery({
    fetchPolicy: 'no-cache', // base option no-cache does not work
    onError(err) {
      setError('Query developer services error ' + err.message)
    }
  })

  const [ getServiceConfig, serviceConfig ] = useDeveloperServiceConfigLazyQuery({
    fetchPolicy: 'no-cache', // base option no-cache does not work
    onError(err) {
      setError('Get service config error ' + err.message)
    }
  })

  const [ setServiceConfig ] =
    useSetDeveloperServiceConfigMutation({
      onError(err) { setError('Set developer service config error: ' + err.message) },
    })

  if (service && !serviceConfig.data && !serviceConfig.loading) {
    getServiceConfig({variables: {service: service}})
  }

  const config = serviceConfig.data?.developerServicesConfig.length?serviceConfig.data.developerServicesConfig[0]:undefined

  if ((!service || config) && developerServices?.developerServicesConfig) {
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
                !!developerServices?.developerServicesConfig.find(s => s.service == service))}
              validate={(environment, repositories, macroValues) =>
                repositories.length > 0 }
              setServiceConfig={(service, environment, repositories, macroValues) =>
                setServiceConfig({ variables: { service, environment, repositories, macroValues } })
                  .then((r) => !!r.data?.setDeveloperServiceConfig) }
              error={error}
              fromUrl={props.fromUrl}
      />)
  } else {
    return null
  }
}

export default DeveloperServiceEditor;
