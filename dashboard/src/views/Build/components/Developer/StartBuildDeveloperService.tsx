import React, {useState} from 'react';

import Button from '@material-ui/core/Button';
import TextField from '@material-ui/core/TextField';
import {NavLink as RouterLink, RouteComponentProps, useHistory, useRouteMatch} from "react-router-dom"

import { makeStyles } from '@material-ui/core/styles';
import {
  Box,
  Card,
  CardContent,
  CardHeader, Checkbox,
  FormControlLabel,
  Grid, Typography
} from '@material-ui/core';
import {
  useBuildDeveloperVersionMutation,
  useDeveloperVersionsInfoLazyQuery,
  useLastCommitCommentMutation,
  useProfileServicesQuery,
  useWhoAmILazyQuery
} from '../../../../generated/graphql';
import Alert from "@material-ui/lab/Alert";
import {Version} from "../../../../common";
import {LogsSubscriber} from "../../../../common/components/logsTable/LogsSubscriber";

const useStyles = makeStyles(theme => ({
  root: {
  },
  controls: {
    marginRight: 16,
    display: 'flex',
    justifyContent: 'flex-end',
    p: 2
  },
  control: {
    marginLeft: '10px',
    textTransform: 'none'
  },
  inProgress: {
    cursor: 'progress',
  },
  alert: {
    marginTop: 25
  }
}));

interface BuildRouteParams {
  service: string
}

interface BuildServiceParams extends RouteComponentProps<BuildRouteParams> {
}

const StartBuildDeveloperService: React.FC<BuildServiceParams> = props => {
  const classes = useStyles()

  const service = props.match.params.service

  const [version, setVersion] = useState('');
  const [comment, setComment] = useState('');
  const [buildClientVersion, setBuildClientVersion] = useState(true);

  const [queryLastCommitComment, setQueryLastCommitComment] = useState(false);
  const [lastCommitComment, setLastCommitComment] = useState(['']);

  const [initialized, setInitialized] = useState(false)

  const [error, setError] = useState<string>()

  const history = useHistory()
  const routeMatch = useRouteMatch()

  const [ getWhoAmI, whoAmI ] = useWhoAmILazyQuery({
    fetchPolicy: 'no-cache', // base option no-cache does not work
    onError(err) { setError('Query who am I error ' + err.message) },
  })
  const [ getDeveloperVersions, developerVersions ] = useDeveloperVersionsInfoLazyQuery({
    fetchPolicy: 'no-cache', // base option no-cache does not work
    variables: { service: service },
    onError(err) { setError('Query developer versions error ' + err.message) },
  })
  const { data: selfServicesProfile } = useProfileServicesQuery({
    fetchPolicy: 'no-cache', // base option no-cache does not work
    variables: { profile: 'self' },
    onError(err) { setError('Query self profile services error ' + err.message) },
  })
  const [ getLastCommitCommentTask, lastCommitCommentTask ] = useLastCommitCommentMutation({
    fetchPolicy: 'no-cache', // base option no-cache does not work
    variables: { service: service },
    onError(err) { setError('Query last commit comment error ' + err.message) },
  })
  const [ buildDeveloperVersion ] = useBuildDeveloperVersionMutation({
    variables: { service: service, version: { build: Version.parseBuild(version) },
      comment: comment, buildClientVersion: buildClientVersion },
    onCompleted(data) {
      history.push(
        `${routeMatch.url.substring(0, routeMatch.url.indexOf("/start"))}/monitor/${data.buildDeveloperVersion}`)
    },
    onError(err) { setError('Build version error ' + err.message) }
  })

  if (!initialized) {
    if (!whoAmI.data && !whoAmI.loading) {
      getWhoAmI()
    }
    if (!developerVersions.data && !developerVersions.loading) {
      getDeveloperVersions()
    }
    if (whoAmI.data && developerVersions.data?.developerVersionsInfo) {
      const lastVersion = developerVersions.data.developerVersionsInfo
        .sort((v1, v2) => v1.buildInfo.time.getTime() > v2.buildInfo.time.getTime() ? -1 : 1)[0]
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
      <Card>
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
              {!queryLastCommitComment && !comment?
                <Button
                  color="primary"
                  variant="contained"
                  onClick={() => {
                    setQueryLastCommitComment(true)
                    getLastCommitCommentTask()
                  }}>
                  Get Last Commit Comment
                </Button>:null}
              {queryLastCommitComment && !comment?
                <Typography>
                  Get Last Commit Comment...
                </Typography>:null}
              {queryLastCommitComment && lastCommitCommentTask.data?
                <LogsSubscriber
                  task={lastCommitCommentTask.data.lastCommitComment}
                  unit={'LAST_COMMENT'}
                  onLines={(lines) => {
                    let comment = lastCommitComment[0]
                    lines.forEach(value => { comment = (comment?comment + '\n':'') + value.message })
                    lastCommitComment[0] = comment
                  }}
                  onComplete={() => {
                    if (!comment) {
                      setComment(lastCommitComment[0])
                    }
                    setQueryLastCommitComment(false)
                  }}
                /> : null}
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
      <div className={classes.root + (queryLastCommitComment? ' ' + classes.inProgress:'')}>
        {BuildCard()}
        {error && <Alert className={classes.alert} severity='error'>{error}</Alert>}
        <Box className={classes.controls}>
          <Button className={classes.control}
            color="primary"
            variant="contained"
            onClick={() => history.goBack()}
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
      </div>) : null
  );
}

export default StartBuildDeveloperService;
