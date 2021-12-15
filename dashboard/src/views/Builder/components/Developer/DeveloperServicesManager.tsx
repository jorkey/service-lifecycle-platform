import React, {useState} from 'react';

import {RouteComponentProps} from 'react-router-dom'

import {
  useDeveloperServiceConfigLazyQuery,
  useDeveloperServicesQuery, useRemoveDeveloperServiceConfigMutation,
  useSetDeveloperServiceConfigMutation
} from "../../../../generated/graphql";
import ServiceEditor from "../ServiceEditor";
import ServicesManager from "../ServicesManager";

interface DeveloperServicesManagerParams {
  fromUrl: string
}

const DeveloperServicesManager: React.FC<DeveloperServicesManagerParams> = props => {
  const { fromUrl } = props

  const [error, setError] = useState<string>()

  const { data: developerServices } = useDeveloperServicesQuery({
    onError(err) {
      setError('Query developer services error ' + err.message)
    }
  })

  const [ removeServiceConfig ] =
    useRemoveDeveloperServiceConfigMutation({
      onError(err) { setError('Remove developer service config error ' + err.message) },
    })


  if (developerServices?.developerServicesConfig) {

    return (<ServicesManager
              title='Developer Services'
              services={developerServices.developerServicesConfig.map(s => s.service)}
              removeServiceConfig={(service) => removeServiceConfig({ variables: { service } }) }
              error={error}
      />)
  } else {
    return null
  }
}

export default DeveloperServiceEditor;
