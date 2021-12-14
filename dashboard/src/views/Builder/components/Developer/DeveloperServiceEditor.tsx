import React, {useState} from 'react';

import {Redirect, RouteComponentProps} from 'react-router-dom'

import {
  NamedStringValue, Repository,
  useDeveloperServicesQuery,
  useSetDeveloperServiceConfigMutation
} from "../../../../generated/graphql";
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
    onError(err) {
      setError('Query service profiles error ' + err.message)
    }
  })

  const [ setServiceConfig ] =
    useSetDeveloperServiceConfigMutation({
      onError(err) { setError('Add service sources error ' + err.message) },
    })

  return developerServices?.developerServicesConfig?
    (<ServiceEditor title=''
                    service={service}
                    environment={}
                    repositories={}
                    macroValues={}
                    hasService={(service => !!developerServices?.developerServicesConfig.find(s => s.service == service))}
                    validate={(service, environment,
                               repositories, macroValues) => true}
                    setServiceConfig={(service, environment,
                               repositories, macroValues) => {}}
                    error={error}
                    fromUrl={props.fromUrl}
    />) : null
}

export default DeveloperServiceEditor;
