import React, {useState} from 'react';

import {
  useBuildClientServicesQuery,
  useRemoveBuildClientServiceConfigMutation,
} from "../../../../../generated/graphql";
import BuildConfiguration from "../BuildConfiguration";

interface ClientServicesManagerParams {
}

const ClientBuildConfiguration: React.FC<ClientServicesManagerParams> = props => {
  const [error, setError] = useState<string>()

  const { data: buildClientServicesConfig, refetch: getBuildClientServicesConfig } = useBuildClientServicesQuery({
    fetchPolicy: 'no-cache', // base option no-cache does not work
    onError(err) {
      setError('Query client services error ' + err.message)
    }
  })

  const [ removeBuildServiceConfig ] =
    useRemoveBuildClientServiceConfigMutation({
      onError(err) { setError('Remove client service config error ' + err.message) },
    })

  if (buildClientServicesConfig?.buildClientServicesConfig) {
    return (<BuildConfiguration
              title='Client Build Configuration'
              services={buildClientServicesConfig.buildClientServicesConfig.map(s => s.service)}
              removeServiceConfig={(service) =>
                removeBuildServiceConfig({ variables: { service } }).then(() => getBuildClientServicesConfig()) }
              error={error}
      />)
  } else {
    return null
  }
}

export default ClientBuildConfiguration;
