import React, {useEffect, useState} from 'react';

import Button from '@material-ui/core/Button';
import TextField from '@material-ui/core/TextField';
import {NavLink as RouterLink, RouteComponentProps, useRouteMatch, useHistory} from "react-router-dom"

import { makeStyles } from '@material-ui/core/styles';
import {
  Box,
  Card,
  CardContent,
  CardHeader,
  Divider,
  Grid
} from '@material-ui/core';
import {
  SourceConfig,
  useBuildDeveloperVersionMutation,
  useDeveloperVersionsInfoLazyQuery,
  useDeveloperVersionsInProcessQuery,
  useServiceSourcesLazyQuery,
  useWhoAmILazyQuery
} from '../../../../generated/graphql';
import clsx from 'clsx';
import Alert from "@material-ui/lab/Alert";
import BranchesTable from "./BranchesTable";
import {Version} from "../../../../common";
import {TaskLogs} from "../../../../common/components/logsTable/TaskLogs";

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

interface BuildRouteParams {
  service: string
}

interface BuildServiceParams extends RouteComponentProps<BuildRouteParams> {
  fromUrl: string
}

const BuildService: React.FC<BuildServiceParams> = props => {
  const classes = useStyles()

  const service = props.match.params.service

  const [version, setVersion] = useState('');
  const [author, setAuthor] = useState('');
  const [sources, setSources] = useState<SourceConfig[]>([]);
  const [comment, setComment] = useState('');

  const [taskId, setTaskId] = useState('')

  const [initialized, setInitialized] = useState(false)

  const [error, setError] = useState<string>()

  const history = useHistory()

  const { data: versionInProcess } = useDeveloperVersionsInProcessQuery({
    variables: { service: service },
    fetchPolicy: 'no-cache',
    onError(err) { setError('Query developer versions in process error ' + err.message) },
    onCompleted() { setError(undefined) }
  })
  const [ getWhoAmI, whoAmI ] = useWhoAmILazyQuery({
    fetchPolicy: 'no-cache',
    onError(err) { setError('Query who am I error ' + err.message) },
    onCompleted() { setError(undefined) }
  })
  const [ getDeveloperVersions, developerVersions ] = useDeveloperVersionsInfoLazyQuery({
    variables: { service: service },
    fetchPolicy: 'no-cache',
    onError(err) { setError('Query developer versions error ' + err.message) },
    onCompleted() { setError(undefined) }
  })
  const [ getServiceSources, serviceSources ] = useServiceSourcesLazyQuery({
    variables: { service: service },
    fetchPolicy: 'no-cache',
    onError(err) { setError('Query service sources error ' + err.message) },
    onCompleted() { setError(undefined) }
  })
  const [ buildDeveloperVersion ] = useBuildDeveloperVersionMutation({
    variables: { service: service, version: { build: Version.parseBuild(version) },
      sources: sources, comment: comment },
    fetchPolicy: 'no-cache',
    onError(err) { setError('Build version error ' + err.message) },
    onCompleted(data) {
      setTaskId(data.buildDeveloperVersion)
      setError(undefined)
    }
  })

  if (!initialized) {
    if (versionInProcess) {
      if (versionInProcess.developerVersionsInProcess?.length) {
        const v = versionInProcess?.developerVersionsInProcess![0]
        setTaskId(v.taskId)
        setVersion(Version.buildToString(v.version.build))
        setAuthor(v.author)
        setSources(v.sources)
        setComment(v.comment)
        setInitialized(true)
      } else {
        if (!whoAmI.data && !whoAmI.loading) {
          getWhoAmI()
        }
        if (!developerVersions.data && !developerVersions.loading) {
          getDeveloperVersions()
        }
        if (!serviceSources.data && !serviceSources.loading) {
          getServiceSources()
        }
        if (whoAmI.data && developerVersions.data && serviceSources.data) {
          const versions = [...developerVersions.data.developerVersionsInfo]
            .sort((v1, v2) =>
              Version.compareBuilds(v1.version.build, v2.version.build))
          const lastVersion = versions.length > 0 ? versions[versions.length-1] : undefined
          if (lastVersion) {
            setVersion(Version.buildToString(Version.nextBuild(lastVersion.version.build)))
          }
          setAuthor(whoAmI.data.whoAmI.user)
          setSources(serviceSources.data.serviceSources)
          setInitialized(true)
        }
      }
    }
  }

  const validateVersion = (version: string) => {
    try {
      Version.parseBuild(version)
      return true
    } catch {
      return false
    }
  }

  const validate = () => {
    return validateVersion(version) && sources.length != 0 && comment.length != 0
  }

  const BuildCard = () => {
    return (
      <Card className={classes.card}>
        <CardHeader title={taskId?`Building Service '${service}'. Author ${author}.`:
          `Build Service '${service}'.`}/>
        <CardContent>
          <Grid container spacing={3}>
            <Grid item md={2} xs={12}>
              <TextField
                fullWidth
                label="Version"
                margin="normal"
                value={version}
                helperText={!validateVersion(version) ? 'Version is not valid': ''}
                error={!validateVersion(version)}
                onChange={(e: any) => setVersion(e.target.value)}
                disabled={!!taskId}
                required
                variant="outlined"
              />
            </Grid>
            <Grid item md={10} xs={12}>
              <TextField
                fullWidth
                label="Comment"
                margin="normal"
                autoFocus
                value={comment}
                variant="outlined"
                onChange={(e: any) => setComment(e.target.value)}
                disabled={!!taskId}
                error={!comment}
              />
            </Grid>
          </Grid>
          <BranchesTable
            branches={sources?.map(source => { return { name: source.name, branch: source.git.branch } })}
            editable={!taskId}
            onBranchesChanged={branches => setSources(sources.map(source => {
              const branch = branches.find(branch => branch.name == source.name)
              return branch ?
                { name: source.name, git: { url: source.git.url, branch: branch.branch, cloneSubmodules: source.git.cloneSubmodules } }
                : source }))
            }
          />
          { taskId ? <TaskLogs taskId={taskId}/> : null }
      </CardContent>
    </Card>)
  }

  return (
    initialized ? (
      <Card
        className={clsx(classes.root)}
      >
        {BuildCard()}
        <Divider />
        {error && <Alert className={classes.alert} severity='error'>{error}</Alert>}
        <Box className={classes.controls}>
          <Button className={classes.control}
            color="primary"
            variant="contained"
            component={RouterLink}
            to={props.fromUrl + '/' /* + props.match.params.type */}
          >
            Cancel
          </Button>
          <Button className={classes.control}
            color="primary"
            variant="contained"
            disabled={!validate()}
            onClick={() => buildDeveloperVersion()}
          >
            Create New Version
          </Button>
        </Box>
      </Card>) : null
  );
}

export default BuildService;
