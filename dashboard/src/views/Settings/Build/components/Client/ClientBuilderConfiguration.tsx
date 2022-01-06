import React, {useState} from 'react';

import {
  useBuildClientServicesQuery,
  useClientBuilderConfigQuery, useRemoveBuildClientServiceConfigMutation,
  useSetClientBuilderConfigMutation,
} from "../../../../../generated/graphql";
import BuilderConfiguration from "../BuilderConfiguration";

interface ClientServicesManagerParams {
}

const ClientBuilderConfiguration: React.FC<ClientServicesManagerParams> = props => {
  const [error, setError] = useState<string>()

  const { data: builderConfig, refetch: getBuilderConfig } = useClientBuilderConfigQuery({
    fetchPolicy: 'no-cache', // base option no-cache does not work
    onError(err) {
      setError('Query client services error ' + err.message)
    }
  })

  const { data: buildClientServicesConfig, refetch: getBuildClientServicesConfig } = useBuildClientServicesQuery({
    fetchPolicy: 'no-cache', // base option no-cache does not work
    onError(err) {
      setError('Query client services error ' + err.message)
    }
  })

  const [ setClientBuilderConfig ] =
    useSetClientBuilderConfigMutation({
      onError(err) { setError('Set client builder config error ' + err.message) },
    })

  const [ removeBuildServiceConfig ] =
    useRemoveBuildClientServiceConfigMutation({
      onError(err) { setError('Remove client service config error ' + err.message) },
    })

  if (builderConfig?.clientBuilderConfig && buildClientServicesConfig?.buildClientServicesConfig) {
    return (<BuilderConfiguration
              title='Client Build Configuration'
              builderConfig={builderConfig.clientBuilderConfig}
              services={buildClientServicesConfig.buildClientServicesConfig.map(s => s.service)}
              setBuilderConfig={(distribution =>
                setClientBuilderConfig({ variables: { distribution: distribution } })
                  .then(() => getBuilderConfig()))}
              removeServiceConfig={(service) =>
                removeBuildServiceConfig({ variables: { service } }).then(() => getBuildClientServicesConfig()) }
              setError={(error) => setError(error)}
              error={error}
      />)
  } else {
    return null
  }
}

export default ClientBuilderConfiguration;
