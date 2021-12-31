import React, {useState} from 'react';
import {RouteComponentProps} from 'react-router-dom'

import {
  useClientServiceConfigLazyQuery,
  useClientServicesQuery,
  useSetClientServiceConfigMutation
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

  const { data: clientServices } = useClientServicesQuery({
    fetchPolicy: 'no-cache', // base option no-cache does not work
    onError(err) {
      setError('Query client services error ' + err.message)
    }
  })

  const [ getServiceConfig, serviceConfig ] = useClientServiceConfigLazyQuery({
    fetchPolicy: 'no-cache', // base option no-cache does not work
    onError(err) {
      setError('Get service config error ' + err.message)
    }
  })

  const [ setServiceConfig ] =
    useSetClientServiceConfigMutation({
      onError(err) { setError('Set client service config error ' + err.message) },
    })

  if (service && !serviceConfig.data && !serviceConfig.loading) {
    getServiceConfig({variables: {service: service}})
  }

  const config = serviceConfig.data?.clientServicesConfig.length?serviceConfig.data.clientServicesConfig[0]:undefined

  if ((!service || config) && clientServices?.clientServicesConfig) {
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
                !!clientServices?.clientServicesConfig.find(s => s.service == service))}
              validate={(environment, repositories, macroValues) =>
                repositories.length > 0 }
              setServiceConfig={(service, environment, repositories, macroValues) =>
                setServiceConfig({ variables: { service, environment, repositories, macroValues } })
                  .then((r) => !!r.data?.setClientServiceConfig)
              }
              error={error}
              fromUrl={props.fromUrl}
      />)
  } else {
    return null
  }
}

export default ClientServiceEditor;
