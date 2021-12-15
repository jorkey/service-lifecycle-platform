import React, {useState} from 'react';

import {
  useDeveloperServicesQuery, useRemoveDeveloperServiceConfigMutation,
} from "../../../../../generated/graphql";
import BuilderConfiguration from "../BuilderConfiguration";

interface DeveloperServicesManagerParams {
}

const DeveloperBuilderConfiguration: React.FC<DeveloperServicesManagerParams> = props => {
  const [error, setError] = useState<string>()

  const { data: developerServices, refetch: getDeveloperServices } = useDeveloperServicesQuery({
    onError(err) {
      setError('Query developer services error ' + err.message)
    }
  })

  const [ removeServiceConfig ] =
    useRemoveDeveloperServiceConfigMutation({
      onError(err) { setError('Remove developer service config error ' + err.message) },
    })

  if (developerServices?.developerServicesConfig) {
    return (<BuilderConfiguration
              title='Developer Services'
              services={developerServices.developerServicesConfig.map(s => s.service)}
              removeServiceConfig={(service) =>
                removeServiceConfig({ variables: { service } }).then(() => getDeveloperServices()) }
              error={error}
      />)
  } else {
    return null
  }
}

export default DeveloperBuilderConfiguration;
