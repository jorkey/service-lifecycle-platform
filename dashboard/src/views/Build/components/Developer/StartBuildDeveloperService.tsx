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
  useBuildDeveloperVersionMutation,
  useDeveloperVersionsInfoLazyQuery, useProfileServicesQuery,
  useWhoAmILazyQuery
} from '../../../../generated/graphql';
import clsx from 'clsx';
import Alert from "@material-ui/lab/Alert";
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

const StartBuildDeveloperService: React.FC<BuildServiceParams> = props => {
  const classes = useStyles()

  const service = props.match.params.service

  const [version, setVersion] = useState('');
  const [comment, setComment] = useState('');
  const [buildClientVersion, setBuildClientVersion] = useState(true);

  const [initialized, setInitialized] = useState(false)

  const [error, setError] = useState<string>()

  const history = useHistory()

  const [ getWhoAmI, whoAmI ] = useWhoAmILazyQuery({
    fetchPolicy: 'no-cache', // base option no-cache does not work
    onError(err) { setError('Query who am I error ' + err.message) },
    onCompleted() { setError(undefined) }
  })
  const [ getDeveloperVersions, developerVersions ] = useDeveloperVersionsInfoLazyQuery({
    fetchPolicy: 'no-cache', // base option no-cache does not work
    variables: { service: service },
    onError(err) { setError('Query developer versions error ' + err.message) },
    onCompleted() { setError(undefined) }
  })
  const { data: selfServicesProfile } = useProfileServicesQuery({
    fetchPolicy: 'no-cache', // base option no-cache does not work
    variables: { profile: 'self' },
    onError(err) { setError('Query self profile services error ' + err.message) },
  })
  const [ buildDeveloperVersion ] = useBuildDeveloperVersionMutation({
    variables: { service: service, version: { build: Version.parseBuild(version) },
      comment: comment, buildClientVersion: buildClientVersion },
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
    if (whoAmI.data && developerVersions.data) {
      const versions = [...developerVersions.data.developerVersionsInfo]
        .sort((v1, v2) =>
          Version.compareBuilds(v1.version.build, v2.version.build))
      const lastVersion = versions.length > 0 ? versions[versions.length-1] : undefined
      if (lastVersion) {
        setVersion(Version.buildToString(Version.nextBuild(lastVersion.version.build)))
      }
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
    return validateVersion(version) && comment.length != 0
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
          {hasClient()?
            <FormControlLabel
              control={(
                <Checkbox
                  color="primary"
                  checked={buildClientVersion}
                  onChange={ event => setBuildClientVersion(event.target.checked) }
                />
              )}
              label="Build Client Version"
            />
            : null}
      </CardContent>
    </Card>)
  }

  const hasClient = () => {
    return selfServicesProfile?.serviceProfiles?.find(profile => profile.services.find(s => s == service))
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

export default StartBuildDeveloperService;
