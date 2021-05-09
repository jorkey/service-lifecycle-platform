import React, {useState} from 'react';

import Button from '@material-ui/core/Button';
import {NavLink as RouterLink, RouteComponentProps, useHistory} from 'react-router-dom'

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
import {ServiceProfileType} from "./ServicesTable";

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

interface ProfileRouteParams {
  profile?: string,
  sourceProfile?: string
}

interface ProfileEditorParams extends RouteComponentProps<ProfileRouteParams> {
  fromUrl: string
}

const ProfileEditor: React.FC<ProfileEditorParams> = props => {
  const {data: profiles} = useServiceProfilesQuery()
  const [getProfileServices, profileServices] = useProfileServicesLazyQuery()

  const classes = useStyles()

  const [profile, setProfile] = useState('');
  const [services, setServices] = useState(new Array<string>());
  const [editColumn, setEditColumn] = useState<{row?: number, column?: string}>(
    { row: undefined, column: undefined });
  const [changed, setChanged] = useState(false);

  const [initialized, setInitialized] = useState(false);

  const editProfile = props.match.params.profile
  const sourceProfile = props.match.params.sourceProfile

  const history = useHistory()

  if (!initialized) {
    if (editProfile) {
      if (!profileServices.data && !profileServices.loading) {
        getProfileServices({variables: {profile: editProfile}})
      }
      if (profileServices.data) {
        setServices([...profileServices.data.serviceProfiles[0].services]
          .sort((s1,s2) => (s1 > s2 ? 1 : -1)))
        setProfile(editProfile)
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
            !sourceProfile?
              (<Box
                className={classes.controls}
              >
                <Button
                  className={classes.control}
                  color="primary"
                  onClick={() => setServices([...services, ''])}
                  startIcon={<AddIcon/>}
                  variant="contained"
                >
                  Add New Service
                </Button>
              </Box>):null
          }
          title={editProfile?'Edit Service Profile':'New Services Profile'}
        />
        { sourceProfile ? (
          <CardContent>
            <Grid
              container
              spacing={3}
            >
              <Grid
                item
                xs={6}
              >
                <ServicesProfile profileType={ServiceProfileType.Projection}
                                 getProfile={() => profile}
                                 doesProfileExist={profile => doesProfileExist(profile)}
                                 getServices={() => services}
                                 setServices={services => {setServices(services); setChanged(true)}}
                                 editColumn={editColumn}
                                 setEditColumn={(row, column) => setEditColumn({row, column})}
                />
              </Grid>
              <Grid
                item
                xs={6}
              >
                <ServicesProfile profileType={ServiceProfileType.Pattern}
                                 getProfile={() => sourceProfile}
                                 getServices={() => []}
                                 editColumn={{row: undefined, column: undefined}}
                                 setEditColumn={() => {}}
                />
              </Grid>
            </Grid>
          </CardContent>) : (
          <CardContent>
            <ServicesProfile profileType={ServiceProfileType.Alone}
                             getProfile={() => profile}
                             doesProfileExist={profile => doesProfileExist(profile)}
                             getServices={() => services}
                             setServices={services => { setServices(services); setChanged(true)}}
                             editColumn={editColumn}
                             setEditColumn={(row, column) => setEditColumn({row, column})}
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
