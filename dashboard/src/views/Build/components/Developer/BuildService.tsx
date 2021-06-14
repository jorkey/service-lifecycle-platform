import React, {useState} from 'react';

import Button from '@material-ui/core/Button';
import TextField from '@material-ui/core/TextField';
import {NavLink as RouterLink, RouteComponentProps, useRouteMatch, useHistory} from "react-router-dom"

import { makeStyles } from '@material-ui/core/styles';
import {Box, Card, CardContent, CardHeader, Checkbox, Divider, FormControlLabel, FormGroup} from '@material-ui/core';
import {
  DeveloperVersion, SourceConfig,
  useAddUserMutation,
  useChangeUserMutation, useDeveloperVersionsInProcessQuery,
  UserRole,
  useUserInfoLazyQuery, useUsersListQuery, useWhoAmIQuery
} from '../../../../generated/graphql';
import clsx from 'clsx';
import Alert from "@material-ui/lab/Alert";
import BranchesTable from "./BranchesTable";

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

  const [version, setVersion] = useState<DeveloperVersion>();
  const [author, setAuthor] = useState<string>('');
  const [sources, setSources] = useState<SourceConfig[]>([]);
  const [comment, setComment] = useState('');

  const [error, setError] = useState<string>()

  const [initialized, setInitialized] = useState(false);

  const history = useHistory()

  const { data: whoAmI } = useWhoAmIQuery()
  const { data: versionInProcess } = useDeveloperVersionsInProcessQuery({
    variables: { service: service },
    fetchPolicy: 'no-cache',
    onError(err) { setError('Query developer versions in process error ' + err.message) },
    onCompleted() { setError(undefined) }
  })

  if (!initialized && whoAmI) {
    if (versionInProcess?.developerVersionsInProcess?.length) {
      const inProcess = versionInProcess?.developerVersionsInProcess![0]
      setVersion(inProcess.version)
      setAuthor(inProcess.author)
      setComment(inProcess.comment)
    }
  }

  const BuildCard = () => {
    return (
      <Card className={classes.card}>
        <CardHeader title={`Build Service ${service}`}/>
        <CardContent>
          <FormGroup row>
            <FormControlLabel
              control={(
                <TextField
                  autoFocus
                  fullWidth
                  margin="normal"
                  value={version}
                  helperText={!editUser && doesUserExist(user) ? 'User already exists': ''}
                  error={!user || (!editUser && doesUserExist(user))}
                  onChange={(e: any) => setUser(e.target.value)}
                  disabled={editUser !== undefined}
                  required
                  variant="outlined"
                />)}
              label="Version"
            />
            <FormControlLabel
              control={(
                <TextField
                  fullWidth
                  margin="normal"
                  value={author}
                  required
                  variant="outlined"
                />)}
              label="Author"
            />
            <FormControlLabel
              control={(
                <BranchesTable
                  branches={sources?.map(source => { return { name: source.name, branch: source.git.branch } })}
                  onBranchesChanged={branches => setSources(sources.map(source => {
                    const branch = branches.find(branch => branch.name == source.name)
                    return branch ?
                      { name: source.name, git: { url: source.git.url, branch: branch.branch, cloneSubmodules: source.git.cloneSubmodules } }
                      : source }))
                  }
                />)}
              label="Branches"
            />
            <FormControlLabel
              control={(
                <TextField
                  fullWidth
                  margin="normal"
                  value={comment}
                  variant="outlined"
                />)}
              label="Comment"
            />
          </FormGroup>
        </CardContent>
      </Card>)
  }

  const validate = () => true

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
            to={props.fromUrl + '/' + props.match.params.type}
          >
            Cancel
          </Button>
          <Button className={classes.control}
            color="primary"
            variant="contained"
            disabled={!validate()}
            onClick={() => submit()}
          > 
            Create New Version
          </Button>
        </Box>
      </Card>) : null
  );
}

export default BuildService;
