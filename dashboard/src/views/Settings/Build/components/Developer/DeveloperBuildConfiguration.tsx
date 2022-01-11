import React, {useState} from 'react';

import {
  useBuildDeveloperServicesQuery,
  useRemoveBuildDeveloperServiceConfigMutation,
} from "../../../../../generated/graphql";
import BuildConfiguration from "../BuildConfiguration";

interface DeveloperServicesManagerParams {
}

const DeveloperBuildConfiguration: React.FC<DeveloperServicesManagerParams> = props => {
  const [error, setError] = useState<string>()

  const { data: developerServices, refetch: getDeveloperServices } = useBuildDeveloperServicesQuery({
    fetchPolicy: 'no-cache', // base option no-cache does not work
    onError(err) {
      setError('Query developer services error ' + err.message)
    }
  })

  const [ removeBuildServiceConfig ] =
    useRemoveBuildDeveloperServiceConfigMutation({
      onError(err) { setError('Remove developer service config error ' + err.message) },
    })

  if (developerServices?.buildDeveloperServicesConfig) {
    return (<BuildConfiguration
              title='Developer Build Configuration'
              services={developerServices.buildDeveloperServicesConfig.map(s => s.service)}
              removeServiceConfig={(service) =>
                removeBuildServiceConfig({ variables: { service } }).then(() => getDeveloperServices()) }
              error={error}
      />)
  } else {
    return null
  }
}

export default DeveloperBuildConfiguration;
