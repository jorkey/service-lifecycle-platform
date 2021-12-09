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
  Divider, Grid,
} from '@material-ui/core';
import {
  useAddServicesProfileMutation,
  useChangeServicesProfileMutation, useDeveloperServicesQuery,
  useProfileServicesLazyQuery,
  useServiceProfilesQuery,
} from '../../../generated/graphql';
import clsx from 'clsx';
import Alert from '@material-ui/lab/Alert';
import AddIcon from '@material-ui/icons/Add';
import ServicesProfile from "./ServicesProfile";
import DevelopmentServices from "./DevelopmentServices";

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

interface ProfileRouteParams {
  profile?: string
}

interface ProfileEditorParams extends RouteComponentProps<ProfileRouteParams> {
  fromUrl: string
}

const ProfileEditor: React.FC<ProfileEditorParams> = props => {
  const [error, setError] = useState<string>()

  const {data: profiles} = useServiceProfilesQuery({
    onError(err) { setError('Query service profiles error ' + err.message) },
    onCompleted() { setError(undefined) }
  })

  const {data: developerServices} = useDeveloperServicesQuery({
    onError(err) { setError('Query developer services error ' + err.message) },
    onCompleted() { setError(undefined) }
  })

  const [getProfileServices, profileServices] = useProfileServicesLazyQuery({
    onError(err) { setError('Query profile services error ' + err.message) },
    onCompleted() { setError(undefined) }
  })

  const classes = useStyles()

  const [profile, setProfile] = useState('');
  const [services, setServices] = useState(new Array<string>());
  const [changed, setChanged] = useState(false);

  const [initialized, setInitialized] = useState(false);
  const [addService, setAddService] = useState(false);

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
    return <Redirect to={props.fromUrl} />
  }

  const validate: () => boolean = () => {
    return !!profile && (!!editProfile || !doesProfileExist(profile)) && services.length != 0
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
      <Card className={classes.card}>
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
                variant="contained"
              >
                Add New Service
              </Button>
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
                               onServiceRemove={service => {
                                 setServices(services.filter(s => s != service))
                                 setChanged(true)
                               }}
              />
            </Grid>
            <Grid
              item
              xs={6}
            >
              <DevelopmentServices services={developerServices?
                                    developerServices?.developerServices.filter(service =>
                                      services.find(s => s == service) === undefined):[]}
                                   deleteIcon={<LeftIcon/>}
                                   onServiceRemove={ service => {
                                     setServices([...services, service])
                                     setChanged(true)
                                   }}
              />
            </Grid>
          </Grid>
        </CardContent>
      </Card>)
  }

  return (
    initialized ? (
      <Card
        className={clsx(classes.root)}
      >
        <ProfileCard />
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
            disabled={!changed || !validate()}
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
