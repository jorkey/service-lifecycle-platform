import React, {useState} from 'react';

import Button from '@material-ui/core/Button';
import {NavLink as RouterLink, Redirect, RouteComponentProps, useHistory} from 'react-router-dom'

import LeftIcon from "@material-ui/icons/ArrowBack";

import { makeStyles } from '@material-ui/core/styles';
import {
  Box,
  Card,
  CardContent,
  CardHeader,
  Grid,
} from '@material-ui/core';
import {
  useAddServicesProfileMutation,
  useChangeServicesProfileMutation, useBuildDeveloperServicesQuery,
  useProfileServicesLazyQuery,
  useServiceProfilesQuery,
} from '../../../../generated/graphql';
import Alert from '@material-ui/lab/Alert';
import AddIcon from '@material-ui/icons/Add';
import ServicesProfile from "./ServicesProfile";
import DevelopmentServices from "./DevelopmentServices";
import DeleteIcon from "@material-ui/icons/Delete";

const useStyles = makeStyles(theme => ({
  controls: {
    marginTop: 25,
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

interface ProfileRouteParams {
  profile?: string
}

interface ProfileEditorParams extends RouteComponentProps<ProfileRouteParams> {
}

const ProfileEditor: React.FC<ProfileEditorParams> = props => {
  const [error, setError] = useState<string>()

  const {data: profiles} = useServiceProfilesQuery({
    fetchPolicy: 'no-cache', // base option no-cache does not work
    onError(err) { setError('Query service profiles error ' + err.message) },
    onCompleted() { setError(undefined) }
  })

  const {data: developerServices} = useBuildDeveloperServicesQuery({
    fetchPolicy: 'no-cache', // base option no-cache does not work
    onError(err) { setError('Query developer services error ' + err.message) },
    onCompleted() { setError(undefined) }
  })

  const [getProfileServices, profileServices] = useProfileServicesLazyQuery({
    fetchPolicy: 'no-cache', // base option no-cache does not work
    onError(err) { setError('Query profile services error ' + err.message) },
    onCompleted() { setError(undefined) }
  })

  const classes = useStyles()

  const [profile, setProfile] = useState('');
  const [services, setServices] = useState(new Array<string>());
  const [changed, setChanged] = useState(false);

  const [initialized, setInitialized] = useState(false);
  const [addService, setAddService] = useState(false);

  const history = useHistory()

  const editProfile = props.match.params.profile

  const requestProfileServices = () => {
    if (editProfile && !profile) {
      if (!profileServices.data && !profileServices.loading) {
        getProfileServices({variables: {profile: editProfile}})
      }
      if (profileServices.data) {
        setProfile(editProfile)
        setServices([...profileServices.data.serviceProfiles[0].services]
          .sort((s1, s2) => (s1 > s2 ? 1 : -1)))
        return true
      } else {
        return false
      }
    } else {
      return true
    }
  }

  if (!initialized) {
    if (requestProfileServices() && developerServices) {
      setInitialized(true)
    }
  }

  const [addProfile, { data: addProfileData, error: addProfileError }] =
    useAddServicesProfileMutation({
      onError(err) { setError('Add services profile error ' + err.message) },
      onCompleted() { setError(undefined) }
    })

  const [changeProfile, { data: changeProfileData, error: changeProfileError }] =
    useChangeServicesProfileMutation({
      onError(err) { setError('Change services profile error ' + err.message) },
      onCompleted() { setError(undefined) }
    })

  if (addProfileData || changeProfileData) {
    history.goBack()
  }

  const validate: () => boolean = () => {
    return !!profile && (!!editProfile || !doesProfileExist(profile))
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

  const ProfileCard = () => {
    return (
      <Card>
        <CardHeader
          action={
            (<Box
              className={classes.controls}
            >
              <Button
                className={classes.control}
                color="primary"
                onClick={() => setAddService(true)}
                startIcon={<AddIcon/>}
                title={'Add service'}
              />
            </Box>)
          }
          title={editProfile?`Edit Service Profile '${editProfile}'`:'New Services Profile'}
        />
        <CardContent>
          <Grid
            container
            spacing={3}
          >
            <Grid
              item
              xs={6}
            >
              <ServicesProfile newProfile={!editProfile}
                               profile={profile}
                               setProfile={setProfile}
                               services={services}
                               doesProfileExist={profile => doesProfileExist(profile)}
                               deleteIcon={<DeleteIcon/>}
                               onServiceRemoved={service =>
                                 new Promise<boolean>(resolve => {
                                   setServices(services.filter(s => s != service))
                                   setChanged(true)
                                   resolve(true)
                               })}
              />
            </Grid>
            <Grid
              item
              xs={6}
            >
              <DevelopmentServices services={developerServices?
                                      developerServices.buildDeveloperServicesConfig.map(s => s.service)
                                        .filter(service => services.find(s => s == service) === undefined):[]}
                                   deleteIcon={<LeftIcon/>}
                                   onServiceRemove={ service =>
                                     new Promise<boolean>(resolve => {
                                       setServices([...services, service])
                                       setChanged(true)
                                       resolve(true)
                                     })
                                   }
              />
            </Grid>
          </Grid>
        </CardContent>
      </Card>)
  }

  return (
    initialized ? (
      <Card>
        <ProfileCard />
        {error && <Alert className={classes.alert} severity="error">{error}</Alert>}
        <Box className={classes.controls}>
          <Button
            className={classes.control}
            color="primary"
            variant="contained"
            onClick={() => history.goBack()}
          >
            Cancel
          </Button>
          <Button
            className={classes.control}
            color="primary"
            disabled={(editProfile && !changed) || !validate()}
            onClick={() => submit()}
            variant="contained"
          >
            {!editProfile?'Add New Profile':'Save'}
          </Button>
        </Box>
      </Card>) : null
  );
}

export default ProfileEditor;
