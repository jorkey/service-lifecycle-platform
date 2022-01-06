import React, {useEffect, useState} from 'react';

import Button from '@material-ui/core/Button';
import {NavLink as RouterLink, Redirect} from 'react-router-dom'

import { makeStyles } from '@material-ui/core/styles';
import {
  Box,
  Card,
  CardContent,
  CardHeader, Select,
} from '@material-ui/core';
import Alert from '@material-ui/lab/Alert';
import AddIcon from '@material-ui/icons/Add';
import {NamedStringValue, Repository, useProvidersInfoQuery} from "../../../../generated/graphql";
import TextField from "@material-ui/core/TextField";
import RepositoriesTable from "./RepositoriesTable";
import NamedValueTable from "./NamedValueTable";
import {FetchResult} from "@apollo/client";
import FormGroup from "@material-ui/core/FormGroup";
import FormControlLabel from "@material-ui/core/FormControlLabel";

const useStyles = makeStyles(theme => ({
  newServiceName: {
    width: 200,
    margin: 0
  },
  providerSelect: {
    width: '150px',
    paddingRight: '2px'
  },
  controls: {
    marginRight: 16,
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
  service?: string
  distribution?: string
  environment: NamedStringValue[]
  repositoriesTitle: string
  repositories: Repository[]
  macroValues: NamedStringValue[]
  hasService: (service: string) => boolean
  validate: (environment: NamedStringValue[], repositories: Repository[], macroValues: NamedStringValue[]) => boolean
  setServiceConfig: (service: string, distribution: string | undefined, environment: NamedStringValue[],
                     repositories: Repository[], macroValues: NamedStringValue[]) => Promise<boolean>
  error?: string
  fromUrl: string
}

const ServiceEditor: React.FC<ServiceEditorParams> = props => {
  const { service: initService, distribution: initBuilerDistribution, environment: initEnvironment,
    repositoriesTitle, repositories: initRepositories, macroValues: initMacroValues,
    hasService, validate, setServiceConfig, error, fromUrl } = props

  const [service, setService] = useState(initService)
  const [builderDistribution, setBuilderDistribution] = useState(initBuilerDistribution)
  const [environment, setEnvironment] = useState(initEnvironment)
  const [repositories, setRepositories] = useState(initRepositories)
  const [macroValues, setMacroValues] = useState(initMacroValues)

  const [addEnvironment, setAddEnvironment] = useState(false)
  const [addRepository, setAddRepository] = useState(false)
  const [addMacroValue, setAddMacroValue] = useState(false)

  const [ownError, setOwnError] = useState<string>()

  const [goBack, setGoBack] = useState(false)

  const classes = useStyles()

  const { data: providers } = useProvidersInfoQuery({
    fetchPolicy: 'no-cache', // base option no-cache does not work
    onError(err) { setOwnError('Query providers info error ' + err.message) },
  })

  useEffect(() => { if (addEnvironment) { setAddRepository(false); setAddMacroValue(false) }},
    [ addEnvironment ])
  useEffect(() => { if (addRepository) { setAddEnvironment(false); setAddMacroValue(false) }},
    [ addRepository ])
  useEffect(() => { if (addMacroValue) { setAddEnvironment(false); setAddRepository(false) }},
    [ addMacroValue ])

  if (goBack) {
    return <Redirect to={fromUrl}/>
  }

  if (providers) {
    const distributions = [localStorage.getItem('distribution'), ...providers.providersInfo.map(info => info.distribution)]

    return (
      <div>
        <Card>
          <CardHeader
            title={initService ? `Edit '${service}' Service Config` : `New Service Config`}
            action={
              !initService?<TextField className={classes.newServiceName}
                         autoFocus
                         error={!!service && hasService(service)}
                         fullWidth
                         helperText={(service && hasService(service)) ? 'Service already exists' : ''}
                         label="Service Name"
                         margin="normal"
                         onChange={e => setService(e.target.value)}
                         required
                         value={service ? service : ''}
                         variant="outlined"
              />:null
            }
          />
        </Card>
        <Card>
          <CardHeader
            action={
              <FormGroup row>
                <FormControlLabel
                  className={classes.control}
                  control={
                    <Select
                      className={classes.providerSelect}
                      native
                      onChange={(event) => {
                        setBuilderDistribution(event.target.value as string)
                      }}
                      value={distributions
                        .find(distribution => distribution == builderDistribution)}
                    >
                      {distributions
                        .map((distribution, index) => <option key={index}>{distribution}</option>)}
                    </Select>
                  }
                  label='Distribution Server To Run The Builder'
                />
              </FormGroup>
            }
            title={'Builder'}
          />
        </Card>
        <Card>
          <CardHeader
            action={
              <Box
                className={classes.controls}
              >
                <Button
                  className={classes.control}
                  color="primary"
                  onClick={() => setAddEnvironment(true)}
                  startIcon={<AddIcon/>}
                  title={'Add variable'}
                />
              </Box>
            }
            title={'Environment'}
          />
          {environment.length || addEnvironment ? <CardContent>
            <NamedValueTable
              values={environment}
              addValue={addEnvironment}
              confirmRemove={true}
              onValueAdded={
                value => {
                  setEnvironment([...environment, value])
                  setAddEnvironment(false)
                }
              }
              onValueAddCancelled={() => {
                setAddEnvironment(false)
              }}
              onValueChanged={
                (oldValue, newValue) => {
                  const newValues = environment.filter(v => v.name != oldValue.name)
                  setEnvironment([...newValues, newValue])
                }
              }
              onValueRemoved={
                value => {
                  const newServices = environment.filter(v => v.name != value.name)
                  setEnvironment(newServices)
                }
              }
            />
          </CardContent> : null}
        </Card>
        <Card>
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
                  title={'Add repository'}
                />
              </Box>
            }
            title={repositoriesTitle}
          />
          <CardContent>
            <RepositoriesTable
              repositories={repositories}
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
                  const newValues = repositories.filter(s => s != repository)
                  setRepositories(newValues)
                }
              }
            />
          </CardContent>
        </Card>
        <Card>
          <CardHeader
            action={
              <Box
                className={classes.controls}
              >
                <Button
                  className={classes.control}
                  color="primary"
                  onClick={() => setAddMacroValue(true)}
                  startIcon={<AddIcon/>}
                  title={'Add value'}
                />
              </Box>
            }
            title={'Macro Values'}
          />
          {macroValues.length || addMacroValue ? <CardContent>
            <NamedValueTable
              values={macroValues}
              addValue={addMacroValue}
              confirmRemove={true}
              onValueAdded={
                value => {
                  setEnvironment([...macroValues, value])
                  setAddMacroValue(false)
                }
              }
              onValueAddCancelled={() => {
                setAddMacroValue(false)
              }}
              onValueChanged={
                (oldValue, newValue) => {
                  const newValues = macroValues.filter(v => v.name != oldValue.name)
                  setMacroValues([...newValues, newValue])
                }
              }
              onValueRemoved={
                value => {
                  const newValues = macroValues.filter(v => v.name != value.name)
                  setMacroValues(newValues)
                }
              }
            />
          </CardContent> : null}
        </Card>
        {ownError && <Alert className={classes.alert} severity="error">{ownError}</Alert>}
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
              setServiceConfig(service!, builderDistribution, environment, repositories, macroValues)
                .then((result) => {
                  if (result) setGoBack(true)
                })
            }}
            variant="contained"
          >
            {!initService ? 'Add New Service' : 'Save'}
          </Button>
        </Box>
    </div>)
  } else {
    return null
  }
}

export default ServiceEditor;
