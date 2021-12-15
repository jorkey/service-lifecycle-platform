import React, {useState} from 'react';

import Button from '@material-ui/core/Button';
import {NavLink as RouterLink, Redirect} from 'react-router-dom'

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
import {NamedStringValue, Repository} from "../../../../generated/graphql";
import TextField from "@material-ui/core/TextField";
import RepositoriesTable from "./RepositoriesTable";

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

interface ServiceEditorParams {
  title: string
  service?: string
  environment: NamedStringValue[]
  repositories: Repository[]
  macroValues: NamedStringValue[]
  hasService: (service: string) => boolean
  validate: (environment: NamedStringValue[], repositories: Repository[], macroValues: NamedStringValue[]) => boolean
  setServiceConfig: (service: string, environment: NamedStringValue[],
                     repositories: Repository[], macroValues: NamedStringValue[]) => Promise<any>
  error?: string
  fromUrl: string
}

const ServiceEditor: React.FC<ServiceEditorParams> = props => {
  const { title, service: initService, environment: initEnvironment,
    repositories: initRepositories, macroValues: initMacroValues,
    hasService, validate, setServiceConfig, error, fromUrl } = props

  const [service, setService] = useState(initService)
  const [environment, setEnvironment] = useState(initEnvironment)
  const [repositories, setRepositories] = useState(initRepositories)
  const [macroValues, setMacroValues] = useState(initMacroValues)

  const [addRepository, setAddRepository] = useState(false);

  const [goBack, setGoBack] = useState(false)

  const classes = useStyles()

  if (goBack) {
    return <Redirect to={fromUrl}/>
  }

  return (
    <Card
      className={clsx(classes.root)}
    >
      <CardHeader
        action={
          <Box
            className={classes.controls}
          >
            <Button
              className={classes.control}
              color="primary"
              onClick={() => setAddRepository(true)}
              startIcon={<AddIcon/>}
              variant="contained"
            >
              Add New Repository
            </Button>
          </Box>
        }
        title={title}
      />
      <CardContent>
        { !initService ?
          <TextField  className={classes.newServiceName}
                      autoFocus
                      error={!!service && hasService(service)}
                      fullWidth
                      helperText={(service && hasService(service)) ? 'Service already exists': ''}
                      label="Service"
                      margin="normal"
                      onChange={e => {setService(e.target.value)}}
                      required
                      value={service?service:''}
                      variant="outlined"
          /> : null }
        <RepositoriesTable repositories={repositories}
                           addRepository={addRepository}
                           confirmRemove={true}
                           onRepositoryAdded={
                             repository => {
                               setRepositories([...repositories, repository])
                               setAddRepository(false)
                             }
                           }
                           onRepositoryAddCancelled={() => {
                             setAddRepository(false)
                           }}
                           onRepositoryChanged={
                             (oldRepository, newRepository) => {
                               const newSources = repositories.filter(s => s != oldRepository)
                               setRepositories([...newSources, newRepository])
                             }
                           }
                           onRepositoryRemoved={
                             repository => {
                               const newServices = repositories.filter(s => s != repository)
                               setRepositories(newServices)
                             }
                           }
        />
      </CardContent>
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
          disabled={!service || !validate(environment, repositories, macroValues)}
          onClick={() => {
            setServiceConfig(service!, environment, repositories, macroValues).then(() => setGoBack(true))
          }}
          variant="contained"
        >
          {!initService?'Add New Service':'Save'}
        </Button>
      </Box>
    </Card>
  )
}

export default ServiceEditor;
