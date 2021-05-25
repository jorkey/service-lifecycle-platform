import React, {useState} from 'react';

import Button from '@material-ui/core/Button';
import {NavLink as RouterLink, RouteComponentProps, useHistory} from 'react-router-dom'

import { makeStyles } from '@material-ui/core/styles';
import {
  Box,
  Card,
  CardContent,
  CardHeader,
  Divider,
} from '@material-ui/core';
import clsx from 'clsx';
import Alert from '@material-ui/lab/Alert';
import AddIcon from '@material-ui/icons/Add';
import {
  SourceConfig,
  useAddServiceSourcesMutation,
  useChangeServiceSourcesMutation, useDeveloperServicesQuery, useServiceSourcesLazyQuery,
} from "../../../../generated/graphql";
import ServiceSources from "./ServiceSources";

const useStyles = makeStyles(theme => ({
  root: {
    padding: theme.spacing(4)
  },
  card: {
    marginTop: 25
  },
  controls: {
    marginTop: 25,
    display: 'flex',
    justifyContent: 'flex-end',
    p: 2
  },
  control: {
    marginLeft: '25px',
    textTransform: 'none'
  },
  alert: {
    marginTop: 25
  }
}));

interface ServiceRouteParams {
  service?: string
}

interface ServiceSourcesEditorParams extends RouteComponentProps<ServiceRouteParams> {
  fromUrl: string
}

const ServiceSourcesEditor: React.FC<ServiceSourcesEditorParams> = props => {
  const {data: services} = useDeveloperServicesQuery({ fetchPolicy: 'no-cache' })
  const [getServiceSources, serviceSources] = useServiceSourcesLazyQuery({ fetchPolicy: 'no-cache' })

  const classes = useStyles()

  const [service, setService] = useState('');
  const [sources, setSources] = useState(new Array<SourceConfig>());
  const [changed, setChanged] = useState(false);

  const [addService, setAddService] = useState(false);

  const editService = props.match.params.service

  const history = useHistory()

  if (editService && !service) {
    if (!serviceSources.data && !serviceSources.loading) {
      getServiceSources({variables: {service: editService}})
    }
    if (serviceSources.data) {
      setService(editService)
      setSources(serviceSources.data.serviceSources)
    }
  }

  const [addSources, { data: addSourcesData, error: addSourcesError }] =
    useAddServiceSourcesMutation({
      onError(err) { console.log(err) }
    })

  const [changeSources, { data: changeSourcesData, error: changeSourcesError }] =
    useChangeServiceSourcesMutation({
      onError(err) { console.log(err) }
    })

  if (addSourcesData || changeSourcesData) {
    history.push(props.fromUrl)
  }

  const validate: () => boolean = () => {
    return !!service && (!!editService || !doesServiceExist(service)) && sources.length != 0
  }

  const submit = () => {
    if (validate()) {
      if (editService) {
        changeSources({variables: { service: service, sources: sources }} )
      } else {
        addSources({variables: { service: service, sources: sources }})
      }
    }
  }

  const doesServiceExist: (service: string) => boolean = (name) => {
    return services?.developerServices?!!services?.developerServices.find(s => s == name):false
  }

  const SourcesCard = () => {
    return (
      <Card className={classes.card}>
        <CardHeader
          action={
            <Box
              className={classes.controls}
            >
              <Button
                className={classes.control}
                color="primary"
                onClick={() => setAddService(true)}
                startIcon={<AddIcon/>}
                variant="contained"
              >
                Add New Source
              </Button>
            </Box>
          }
          title={editService?`Edit Development Service '${editService}'`:'New Development Service'}
        />
        <CardContent>
          <ServiceSources  newService={!editService}
                           service={service}
                           setService={setService}
                           doesServiceExist={doesServiceExist}
                           sources={sources}
                           addSource={addService}
                           confirmRemove={true}
                           onSourceAdded={
                             source => {
                               setSources([...sources, source])
                               setAddService(false)
                               setChanged(true)
                             }
                           }
                           onSourceAddCancelled={() => {
                             setAddService(false)
                           }}
                           onSourceChange={
                             (oldSource, newSource) => {
                               const newSources = sources.filter(s => s != oldSource)
                               setSources([...newSources, newSource])
                               setChanged(true)}
                           }
                           onSourceRemove={
                             source => {
                               const newServices = sources.filter(s => s != source)
                               setSources(newServices)
                               setChanged(true)
                             }
                           }
          />
        </CardContent>
      </Card>)
  }

  const error = addSourcesError?addSourcesError.message:changeSourcesError?changeSourcesError.message:''

  return (
    <Card
      className={clsx(classes.root)}
    >
      <SourcesCard />
      <Divider />
      {error && <Alert
        className={classes.alert}
        severity="error"
      >{error}</Alert>}
      <Box className={classes.controls}>
        <Button
          className={classes.control}
          color="primary"
          component={RouterLink}
          to={props.fromUrl}
          variant="contained"
        >
          Cancel
        </Button>
        <Button
          className={classes.control}
          color="primary"
          disabled={!changed || !validate()}
          onClick={() => submit()}
          variant="contained"
        >
          {!editService?'Add New Service':'Save'}
        </Button>
      </Box>
    </Card>
  );
}

export default ServiceSourcesEditor;
