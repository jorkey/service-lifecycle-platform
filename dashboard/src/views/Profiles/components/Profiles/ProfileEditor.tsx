import React, {useState} from 'react';

import Button from '@material-ui/core/Button';
import TextField from '@material-ui/core/TextField';
import {NavLink as RouterLink, RouteComponentProps, useHistory} from 'react-router-dom'

import { makeStyles } from '@material-ui/core/styles';
import {Box, Card, CardContent, CardHeader, Divider} from '@material-ui/core';
import {
  useAddServicesProfileMutation,
  useChangeServicesProfileMutation,
  useProfileServicesLazyQuery,
  useServiceProfilesQuery,
} from '../../../../generated/graphql';
import clsx from 'clsx';
import Alert from '@material-ui/lab/Alert';

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
    marginLeft: '25px'
  },
  alert: {
    marginTop: 25
  }
}));

interface UserRouteParams {
  profile?: string
}

interface UserEditorParams extends RouteComponentProps<UserRouteParams> {
  fromUrl: string
}

const ProfileEditor: React.FC<UserEditorParams> = props => {
  const {data: profiles} = useServiceProfilesQuery()
  const [getProfileServices, profileServices] = useProfileServicesLazyQuery()

  const classes = useStyles()

  const [profile, setProfile] = useState('');
  const [services, setServices] = useState(new Array<string>());

  const [initialized, setInitialized] = useState(false);

  const editProfile = props.match.params.profile

  const history = useHistory()

  if (!initialized) {
    if (editProfile) {
      if (!profileServices.data && !profileServices.loading) {
        getProfileServices({variables: {profile: editProfile}})
      }
      if (profileServices.data) {
        setServices(profileServices.data.serviceProfiles[0].services)
        setInitialized(true)
      }
    } else {
      setInitialized(true)
    }
  }

  const [addProfile, { data: addProfileData, error: addProfileError }] =
    useAddServicesProfileMutation({
      onError(err) { console.log(err) }
    })

  const [changeProfile, { data: changeProfileData, error: changeProfileError }] =
    useChangeServicesProfileMutation({
      onError(err) { console.log(err) }
    })

  if (addProfileData || changeProfileData) {
    history.push(props.fromUrl)
  }

  const validate: () => boolean = () => {
    return !!profile && !doesProfileExist(profile) && services.length != 0
  }

  const submit = () => {
    if (validate()) {
      if (editProfile) {
        changeProfile({variables: { profile: profile, services: services }} )
      } else {
        addProfile({variables: { profile: profile, services: services }})
      }
    }
  }

  const doesProfileExist: (profile: string) => boolean = (profile) => {
    return profiles?!!profiles.serviceProfiles.find(p => p.profile == profile):false
  }

  const UserCard = () => {
    return (
      <Card className={classes.card}>
        <CardHeader title={editProfile?'Edit Service Profile':'New Services Profile'}/>
        <CardContent>
          <TextField
            autoFocus
            fullWidth
            label="Profile"
            margin="normal"
            value={profile}
            helperText={!editProfile && doesProfileExist(profile) ? 'Profile already exists': ''}
            error={!profile || (!editProfile && doesProfileExist(profile))}
            onChange={(e: any) => setProfile(e.target.value)}
            disabled={editProfile !== undefined}
            required
            variant="outlined"
          />
        </CardContent>
      </Card>)
  }

  const error = addProfileError?addProfileError.message:changeProfileError?changeProfileError.message:''

  return (
    initialized ? (
      <Card
        className={clsx(classes.root)}
      >
        {UserCard()}
        <Divider />
        {error && <Alert className={classes.alert} severity='error'>{error}</Alert>}
        <Box className={classes.controls}>
          <Button className={classes.control}
            color="primary"
            variant="contained"
            component={RouterLink}
            to={props.fromUrl}
          >
            Cancel
          </Button>
          <Button className={classes.control}
            color="primary"
            variant="contained"
            disabled={!validate()}
            onClick={() => submit()}
          >
            {!editProfile?'Add New User':'Save'}
          </Button>
        </Box>
      </Card>) : null
  );
}

export default ProfileEditor;
