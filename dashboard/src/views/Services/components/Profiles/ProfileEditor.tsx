import React, {useState} from 'react';

import Button from '@material-ui/core/Button';
import {NavLink as RouterLink, RouteComponentProps, useHistory} from 'react-router-dom'

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
  useChangeServicesProfileMutation,
  useProfileServicesLazyQuery,
  useServiceProfilesQuery,
} from '../../../../generated/graphql';
import clsx from 'clsx';
import Alert from '@material-ui/lab/Alert';
import AddIcon from '@material-ui/icons/Add';
import ServicesProfile from "./ServicesProfile";

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
  const {data: profiles} = useServiceProfilesQuery({ fetchPolicy: 'no-cache' })
  const [getProfileServices, profileServices] = useProfileServicesLazyQuery({ fetchPolicy: 'no-cache' })
  const [getPatternProfileServices, patternProfileServices] = useProfileServicesLazyQuery({ fetchPolicy: 'no-cache' })

  const classes = useStyles()

  const [profile, setProfile] = useState('');
  const [services, setServices] = useState(new Array<string>());
  const [patternServices, setPatternServices] = useState(new Array<string>());
  const [changed, setChanged] = useState(false);

  const [initialized, setInitialized] = useState(false);
  const [addService, setAddService] = useState(false);

  const editProfile = props.match.params.profile
  const patternProfile = (editProfile != 'common') ? 'common' : undefined

  const history = useHistory()

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

  const requestPatternProfileServices = () => {
    if (patternProfile && !patternServices.length) {
      if (!patternProfileServices.data && !patternProfileServices.loading) {
        getPatternProfileServices({variables: {profile: patternProfile}})
      }
      if (patternProfileServices.data) {
        if (patternProfileServices.data.serviceProfiles.length) {
          setPatternServices([...patternProfileServices.data.serviceProfiles[0].services]
            .sort((s1, s2) => (s1 > s2 ? 1 : -1)))
        }
        return true
      } else {
        return false
      }
    } else {
      return true
    }
  }

  if (!initialized) {
    const r1 = requestProfileServices()
    const r2 = requestPatternProfileServices()
    if (r1 && r2) {
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
            !patternProfile?
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
              </Box>):null
          }
          title={editProfile?`Edit Service Profile '${editProfile}'`:'New Services Profile'}
        />
        { patternProfile ? (
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
                <ServicesProfile profile={patternProfile}
                                 services={patternServices.filter(service => !services.find(s => s == service))}
                                 deleteIcon={<LeftIcon/>}
                                 onServiceRemove={ service => {
                                   setServices([...services, service])
                                   setChanged(true)
                                 }}
                />
              </Grid>
            </Grid>
          </CardContent>) : (
          <CardContent>
            <ServicesProfile newProfile={!editProfile}
                             profile={profile}
                             setProfile={setProfile}
                             doesProfileExist={profile => doesProfileExist(profile)}
                             services={services}
                             addService={addService}
                             allowEdit={true}
                             confirmRemove={true}
                             onServiceAdded={
                               service => {
                                 setServices([...services, service].sort((s1,s2) => (s1 > s2 ? 1 : -1)))
                                 setAddService(false)
                                 setChanged(true)
                               }
                             }
                             onServiceAddCancelled={() => {
                               setAddService(false)
                             }}
                             onServiceChange={
                               (oldService, newService) => {
                                 const newServices = services.filter(s => s != oldService)
                                 setServices([...newServices, newService].sort((s1,s2) => (s1 > s2 ? 1 : -1)));
                                 setChanged(true)}
                             }
                             onServiceRemove={
                               service => {
                                 const newServices = services.filter(s => s != service)
                                 setServices(newServices.sort((s1,s2) => (s1 > s2 ? 1 : -1)));
                                 setChanged(true)
                               }
                             }
            />
          </CardContent>
        )}
      </Card>)
  }

  const error = addProfileError?addProfileError.message:changeProfileError?changeProfileError.message:''

  return (
    initialized ? (
      <Card
        className={clsx(classes.root)}
      >
        <ProfileCard />
        <Divider />
        {error && <Alert
          className={classes.alert}
          severity="error"
        >{error}</Alert>}
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
