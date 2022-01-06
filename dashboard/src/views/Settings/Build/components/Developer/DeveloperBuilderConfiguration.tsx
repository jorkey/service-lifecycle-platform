import React, {useState} from 'react';

import {
  useBuildDeveloperServicesQuery,
  useDeveloperBuilderConfigQuery, useRemoveBuildDeveloperServiceConfigMutation,
  useSetDeveloperBuilderConfigMutation,
} from "../../../../../generated/graphql";
import BuilderConfiguration from "../BuilderConfiguration";

interface DeveloperServicesManagerParams {
}

const DeveloperBuilderConfiguration: React.FC<DeveloperServicesManagerParams> = props => {
  const [error, setError] = useState<string>()

  const { data: builderConfig, refetch: getBuilderConfig } = useDeveloperBuilderConfigQuery({
    fetchPolicy: 'no-cache', // base option no-cache does not work
    onError(err) {
      setError('Query developer services error ' + err.message)
    }
  })

  const { data: developerServices, refetch: getDeveloperServices } = useBuildDeveloperServicesQuery({
    fetchPolicy: 'no-cache', // base option no-cache does not work
    onError(err) {
      setError('Query developer services error ' + err.message)
    }
  })

  const [ setDeveloperBuilderConfig ] =
    useSetDeveloperBuilderConfigMutation({
      onError(err) { setError('Set developer builder config error ' + err.message) },
    })

  const [ removeBuildServiceConfig ] =
    useRemoveBuildDeveloperServiceConfigMutation({
      onError(err) { setError('Remove developer service config error ' + err.message) },
    })

  if (builderConfig?.developerBuilderConfig && developerServices?.buildDeveloperServicesConfig) {
    return (<BuilderConfiguration
              title='Developer Build Configuration'
              builderConfig={builderConfig.developerBuilderConfig}
              services={developerServices.buildDeveloperServicesConfig.map(s => s.service)}
              setBuilderConfig={(distribution =>
                setDeveloperBuilderConfig({ variables: { distribution: distribution } })
                  .then(() => getBuilderConfig()))}
              removeServiceConfig={(service) =>
                removeBuildServiceConfig({ variables: { service } }).then(() => getDeveloperServices()) }
              setError={(error) => setError(error)}
              error={error}
      />)
  } else {
    return null
  }
}

export default DeveloperBuilderConfiguration;
