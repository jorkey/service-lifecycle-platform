import React, {useEffect, useState} from 'react';

import Button from '@material-ui/core/Button';
import {NavLink as RouterLink, Redirect, RouteComponentProps, useHistory} from 'react-router-dom'

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
import TextField from "@material-ui/core/TextField";
import SourcesTable from "./SourcesTable";

const useStyles = makeStyles(theme => ({
  root: {
    padding: theme.spacing(4)
  },
  card: {
    marginTop: 25
  },
  newServiceName: {
    height: 60,
    margin: 0
  },
  serviceName: {
    paddingLeft: 10,
    paddingTop: 15,
    height: 60,
    margin: 0
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

interface SourcesCardParams {
  editService: string | undefined
  onServiceChanged: (service: string, sources: SourceConfig[], readyToSave: boolean) => void
}

const SourcesCard = (params: SourcesCardParams) => {
  const { editService, onServiceChanged } = params
  const classes = useStyles()

  const {data: services} = useDeveloperServicesQuery()
  const [getServiceSources, serviceSources] = useServiceSourcesLazyQuery()

  const [service, setService] = useState('');
  const [sources, setSources] = useState(new Array<SourceConfig>());

  const [addSource, setAddSource] = useState(false);

  useEffect(() => {
    onServiceChanged(service, sources, validate())
  }, [service, sources])

  const doesServiceExist: (service: string) => boolean = (name) => {
    return services?.developerServices?!!services?.developerServices.find(s => s == name):false
  }

  const validate: () => boolean = () => {
    return !!service && (!!editService || !doesServiceExist(service)) && sources.length != 0
  }

  if (editService && !service) {
    if (!serviceSources.data && !serviceSources.loading) {
      getServiceSources({variables: {service: editService}})
    }
    if (serviceSources.data) {
      setService(editService)
      setSources(serviceSources.data.serviceSources)
    }
  }

  return (<div>
    <Card className={classes.card}>
      <CardHeader
        action={
          <Box
            className={classes.controls}
          >
            <Button
              className={classes.control}
              color="primary"
              onClick={() => setAddSource(true)}
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
        { !editService ?
          <TextField  className={classes.newServiceName}
                      autoFocus
                      error={!!service && doesServiceExist?.(service)}
                      fullWidth
                      helperText={(service && doesServiceExist?.(service)) ? 'Service already exists': ''}
                      label="Service"
                      margin="normal"
                      onChange={e => {setService(e.target.value)}}
                      required
                      value={service?service:''}
                      variant="outlined"
          /> : null }
        <SourcesTable  sources={sources}
                       addSource={addSource}
                       confirmRemove={true}
                       onSourceAdded={
                         source => {
                           setSources([...sources, source])
                           setAddSource(false)
                         }
                       }
                       onSourceAddCancelled={() => {
                         setAddSource(false)
                       }}
                       onSourceChanged={
                         (oldSource, newSource) => {
                           const newSources = sources.filter(s => s != oldSource)
                           setSources([...newSources, newSource])
                         }
                       }
                       onSourceRemoved={
                         source => {
                           const newServices = sources.filter(s => s != source)
                           setSources(newServices)
                         }
                       }
        />
      </CardContent>
    </Card></div>)
}

interface ServiceRouteParams {
  service?: string
}

interface ServiceSourcesEditorParams extends RouteComponentProps<ServiceRouteParams> {
    fromUrl: string
}

const ServiceSourcesEditor: React.FC<ServiceSourcesEditorParams> = props => {
  const classes = useStyles()

  const [service, setService] = useState('');
  const [sources, setSources] = useState(new Array<SourceConfig>());
  const [readyToSave, setReadyToSave] = useState(false);

  const editService = props.match.params.service

  const [error, setError] = useState<string>()

  const [addSources, { data: addSourcesData, error: addSourcesError }] =
    useAddServiceSourcesMutation({
      onError(err) { setError('Add service sources error ' + err.message) },
      onCompleted() { setError(undefined) }
    })

  const [changeSources, { data: changeSourcesData, error: changeSourcesError }] =
    useChangeServiceSourcesMutation({
      onError(err) { setError('Change service sources error ' + err.message) },
      onCompleted() { setError(undefined) }
    })

  if (addSourcesData || changeSourcesData) {
    return <Redirect to={props.fromUrl} />
  }

  const submit = () => {
    if (readyToSave) {
      if (editService) {
        changeSources({variables: { service: service, sources: sources }} )
      } else {
        addSources({variables: { service: service, sources: sources }})
      }
    }
  }

  return (
    <Card
      className={clsx(classes.root)}
    >
      <SourcesCard editService={editService} onServiceChanged={(service, sources, readyToSave) => {
        setService(service)
        setSources(sources)
        setReadyToSave(readyToSave)}
      }/>
      <Divider />
      {error && <Alert className={classes.alert} severity="error">{error}</Alert>}
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
          disabled={!readyToSave}
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
