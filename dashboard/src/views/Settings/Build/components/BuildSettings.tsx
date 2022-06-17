import React, {useEffect, useState} from 'react';

import Button from '@material-ui/core/Button';
import {NavLink as RouterLink, Redirect, useHistory} from 'react-router-dom'

import { makeStyles } from '@material-ui/core/styles';
import {
  Box,
  Card,
  CardContent,
  CardHeader, Select, Typography,
} from '@material-ui/core';
import Alert from '@material-ui/lab/Alert';
import AddIcon from '@material-ui/icons/Add';
import {FileInfo, NamedStringValue, Repository, useProvidersInfoQuery} from "../../../../generated/graphql";
import TextField from "@material-ui/core/TextField";
import RepositoriesTable from "./RepositoriesTable";
import NamedValueTable from "./NamedValueTable";
import FormGroup from "@material-ui/core/FormGroup";
import FormControlLabel from "@material-ui/core/FormControlLabel";
import PrivateFilesTable from "./PrivateFilesTable";
import {upload} from "../../../../common/load/Upload";

const useStyles = makeStyles(theme => ({
  serviceRecord: {
    display: 'flex'
  },
  label: {
    marginTop: '25px',
    marginLeft: '16px',
    marginBottom: '10px',
    paddingTop: '10px',
    fontSize: '18px',
    fontWeight: 500,
  },
  service: {
    width: 200,
    marginTop: '25px',
    marginLeft: '16px'
  },
  providerSelect: {
    width: '200px',
    paddingRight: '2px'
  },
  controls: {
    marginRight: 16,
    display: 'flex',
    justifyContent: 'flex-end',
    p: 2
  },
  control: {
    marginLeft: '10px',
  },
  alert: {
    marginTop: 25
  }
}));

interface ServiceEditorParams {
  service?: string
  services?: string[]
  distribution?: string | null
  environment: NamedStringValue[]
  repositoriesTitle: string
  repositories: Repository[]
  privateFiles: FileInfo[]
  uploadPrivateFilePath: string
  macroValues: NamedStringValue[]
  hasService: (service: string) => boolean
  validate: (environment: NamedStringValue[], repositories: Repository[], macroValues: NamedStringValue[]) => boolean
  setServiceConfig: (service: string, distribution: string | null | undefined, environment: NamedStringValue[],
                     repositories: Repository[], privateFiles: FileInfo[], macroValues: NamedStringValue[]) => Promise<boolean>
  error?: string
}

const BuildSettings: React.FC<ServiceEditorParams> = props => {
  const { service: initService, services, distribution: initBuilderDistribution, environment: initEnvironment,
    repositoriesTitle, repositories: initRepositories, privateFiles: initPrivateFiles, uploadPrivateFilePath,
    macroValues: initMacroValues, hasService, validate, setServiceConfig, error } = props

  const [service, setService] = useState(initService)
  const [builderDistribution, setBuilderDistribution] = useState(initBuilderDistribution)
  const [environment, setEnvironment] = useState(initEnvironment)
  const [repositories, setRepositories] = useState(initRepositories)
  const [privateFiles, setPrivateFiles] = useState(initPrivateFiles)
  const [macroValues, setMacroValues] = useState(initMacroValues)

  const [addEnvironment, setAddEnvironment] = useState(false)
  const [addRepository, setAddRepository] = useState(false)
  const [addPrivateFile, setAddPrivateFile] = useState(false)
  const [addMacroValue, setAddMacroValue] = useState(false)

  const [filesToUpload, setFilesToUpload] = useState(new Map<string, File>())

  const [ownError, setOwnError] = useState<string>()

  const [goBack, setGoBack] = useState(false)

  const classes = useStyles()

  const { data: providers } = useProvidersInfoQuery({
    fetchPolicy: 'no-cache', // base option no-cache does not work
    onError(err) { setOwnError('Query providers info error ' + err.message) },
  })

  const history = useHistory()

  useEffect(() => { if (addEnvironment) { setAddRepository(false); setAddMacroValue(false) }},
    [ addEnvironment ])
  useEffect(() => { if (addRepository) { setAddEnvironment(false); setAddMacroValue(false) }},
    [ addRepository ])
  useEffect(() => { if (addMacroValue) { setAddEnvironment(false); setAddRepository(false) }},
    [ addMacroValue ])

  if (goBack) {
    history.goBack()
  }

  if (providers) {
    const distributions = [localStorage.getItem('distribution')!,
      ...providers.providersInfo.map(info => info.distribution)]

    return (
      <div>
        <div className={classes.serviceRecord}>
        {initService == undefined?
          <>
            <Typography className={classes.label}>
              {'Service'}
            </Typography>
            {services?
             <Select
               className={classes.service}
               native
               onChange={e => setService(e.target.value ? e.target.value as string : undefined)}
             >
               <option key={-1}></option>)
               {services.sort().map((service, index) =>
                 <option key={index}>{service}</option>)}
             </Select>:
             <TextField className={classes.service}
                        autoFocus
                        error={!!service && hasService(service)}
                        fullWidth
                        helperText={(service && hasService(service)) ? 'Service already exists' : ''}
                        margin="normal"
                        onChange={e => setService(e.target.value)}
                        required
                        value={service ? service : ''}
             />}
           </>:
             <Typography className={classes.label}>
               {service?`Service '${service}' Build Settings`:'Common Build Settings'}
             </Typography>
        }
        </div>
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
                        setBuilderDistribution(event.target.value?event.target.value as string:undefined)
                      }}
                      value={distributions
                        .find(distribution => distribution == builderDistribution)}
                    >
                      {['', ...distributions]
                        .map((distribution, index) => <option key={index}>{distribution}</option>)}
                    </Select>
                  }
                  label={''}
                />
              </FormGroup>
            }
            title={'Distribution Server To Run Build'}
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
                (index, newValue) => {
                  environment[index] = newValue
                  setEnvironment([...environment])
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
          {repositories.length || addRepository? <CardContent>
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
                (index, newRepository) => {
                  repositories[index] = newRepository
                  setRepositories([...repositories])
                }
              }
              onRepositoryRemoved={
                repository => {
                  const newValues = repositories.filter(s => s != repository)
                  setRepositories(newValues)
                }
              }
            />
          </CardContent>:null}
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
                  onClick={() => setAddPrivateFile(true)}
                  startIcon={<AddIcon/>}
                  title={'Add private file'}
                />
              </Box>
            }
            title={'Private Files'}
          />
          {privateFiles.length || addPrivateFile?<CardContent>
            <PrivateFilesTable
              privateFiles={privateFiles}
              filesToUpload={filesToUpload}
              addPrivateFile={addPrivateFile}
              confirmRemove={true}
              onPrivateFileAdded={
                (path, localFile) => {
                  setPrivateFiles([...privateFiles,
                    {path: path, time: new Date(localFile.lastModified), length: localFile.size}])
                  setFilesToUpload(new Map(filesToUpload.set(path, localFile)))
                  setAddPrivateFile(false)
                }
              }
              onPrivateFileAddCancelled={() => {
                setAddPrivateFile(false)
              }}
              onPrivateFileRemoved={
                path => setPrivateFiles(privateFiles.filter(s => s.path != path))
              }
            />
          </CardContent>:null}
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
                  setMacroValues([...macroValues, value])
                  setAddMacroValue(false)
                }
              }
              onValueAddCancelled={() => {
                setAddMacroValue(false)
              }}
              onValueChanged={
                (index, newValue) => {
                  macroValues[index] = newValue
                  setMacroValues([...macroValues])
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
            onClick={() => history.goBack()}
          >
            Cancel
          </Button>
          <Button
            className={classes.control}
            color="primary"
            disabled={service == undefined ||
              (builderDistribution == initBuilderDistribution &&
                environment == initEnvironment &&
                repositories == initRepositories &&
                privateFiles == initPrivateFiles &&
                macroValues == initMacroValues)  ||
              !validate(environment, repositories, macroValues)}
            onClick={() => {
              Promise.all(Array.from(filesToUpload).map(([path, file]) =>
                upload(uploadPrivateFilePath + '/' + encodeURIComponent(service + '/' + path), file)))
              .then(() => setServiceConfig(service!, builderDistribution, environment, repositories,
                  privateFiles, macroValues))
              .then((result) => { if (result) setGoBack(true) })
              .catch(result => setOwnError(JSON.stringify(result)))
            }}
            variant="contained"
          >
            {initService == undefined ? 'Add New Service' : 'Save'}
          </Button>
        </Box>
    </div>)
  } else {
    return null
  }
}

export default BuildSettings;
