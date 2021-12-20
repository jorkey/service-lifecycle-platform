import React, {useState} from 'react';

import {
  useDeveloperBuilderConfigQuery,
  useDeveloperServicesQuery, useRemoveDeveloperServiceConfigMutation, useSetDeveloperBuilderConfigMutation,
} from "../../../../../generated/graphql";
import BuilderConfiguration from "../BuilderConfiguration";

interface DeveloperServicesManagerParams {
}

const DeveloperBuilderConfiguration: React.FC<DeveloperServicesManagerParams> = props => {
  const [error, setError] = useState<string>()

  const { data: builderConfig, refetch: getBuilderConfig } = useDeveloperBuilderConfigQuery({
    onError(err) {
      setError('Query developer services error ' + err.message)
    }
  })

  const { data: developerServices, refetch: getDeveloperServices } = useDeveloperServicesQuery({
    onError(err) {
      setError('Query developer services error ' + err.message)
    }
  })

  const [ setDeveloperBuilderConfig ] =
    useSetDeveloperBuilderConfigMutation({
      onError(err) { setError('Set developer builder config error ' + err.message) },
    })

  const [ removeServiceConfig ] =
    useRemoveDeveloperServiceConfigMutation({
      onError(err) { setError('Remove developer service config error ' + err.message) },
    })

  if (builderConfig?.developerBuilderConfig && developerServices?.developerServicesConfig) {
    return (<BuilderConfiguration
              title='Developer Build Configuration'
              builderConfig={builderConfig.developerBuilderConfig}
              services={developerServices.developerServicesConfig.map(s => s.service)}
              setBuilderConfig={(distribution =>
                setDeveloperBuilderConfig({ variables: { distribution: distribution } })
                  .then(() => {
                    console.log('getBuilderConfig')
                    getBuilderConfig()}))}
              removeServiceConfig={(service) =>
                removeServiceConfig({ variables: { service } }).then(() => getDeveloperServices()) }
              setError={(error) => setError(error)}
              error={error}
      />)
  } else {
    return null
  }
}

export default DeveloperBuilderConfiguration;
