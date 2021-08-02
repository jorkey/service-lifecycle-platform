import React, {useState} from 'react';

import Button from '@material-ui/core/Button';
import TextField from '@material-ui/core/TextField';
import {NavLink as RouterLink, RouteComponentProps, useHistory} from "react-router-dom"

import { makeStyles } from '@material-ui/core/styles';
import {
  Box,
  Card,
  CardContent,
  CardHeader, Checkbox,
  Divider, FormControlLabel,
  Grid
} from '@material-ui/core';
import {
  SourceConfig,
  useBuildDeveloperVersionMutation,
  useDeveloperVersionsInfoLazyQuery, useProfileServicesQuery, useServiceProfilesQuery,
  useServiceSourcesLazyQuery,
  useWhoAmILazyQuery
} from '../../../../generated/graphql';
import clsx from 'clsx';
import Alert from "@material-ui/lab/Alert";
import BranchesTable from "./BranchesTable";
import {Version} from "../../../../common";

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

const StartBuildService: React.FC<BuildServiceParams> = props => {
  const classes = useStyles()

  const service = props.match.params.service

  const [version, setVersion] = useState('');
  const [author, setAuthor] = useState('');
  const [sources, setSources] = useState<SourceConfig[]>([]);
  const [comment, setComment] = useState('');
  const [allowBuildClientVersion, setAllowBuildClientVersion] = useState(true);

  const [initialized, setInitialized] = useState(false)

  const [error, setError] = useState<string>()

  const history = useHistory()

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
  const { data: ownServicesProfile } = useProfileServicesQuery({
    variables: { profile: 'own' },
    fetchPolicy: 'no-cache',
    onError(err) { setError('Query own profile services error ' + err.message) },
  })
  const [ buildDeveloperVersion ] = useBuildDeveloperVersionMutation({
    variables: { service: service, version: { build: Version.parseBuild(version) },
      sources: sources, comment: comment },
    fetchPolicy: 'no-cache',
    onError(err) { setError('Build version error ' + err.message) },
    onCompleted(data) {
      history.push(props.fromUrl + '/monitor/' + service)
    }
  })

  if (!initialized) {
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
      setAuthor(whoAmI.data.whoAmI.account)
      setSources(serviceSources.data.serviceSources)
      setInitialized(true)
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
        <CardHeader title={`Start Build Service '${service}'.`}/>
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
                error={!comment}
              />
            </Grid>
          </Grid>
          <BranchesTable
            branches={sources?.map(source => { return { name: source.name, branch: source.git.branch } })}
            editable={true}
            onBranchesChanged={branches => setSources(sources.map(source => {
              const branch = branches.find(branch => branch.name == source.name)
              return branch ?
                { name: source.name, git: { url: source.git.url, branch: branch.branch, cloneSubmodules: source.git.cloneSubmodules } }
                : source }))
            }
          />
          {hasClient()?
            <FormControlLabel
              control={(
                <Checkbox
                  color="primary"
                  checked={allowBuildClientVersion}
                  onChange={ event => setAllowBuildClientVersion(event.target.checked) }
                />
              )}
              label="Build Client Version"
            />
            : null}
      </CardContent>
    </Card>)
  }

  const hasClient = () => {
    return ownServicesProfile?.serviceProfiles?.find(profile => profile.services.find(s => s == service))
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
            to={ props.fromUrl }
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

export default StartBuildService;
