import TextField from "@material-ui/core/TextField";
import React, {useState} from "react";
import {ServicesTable} from "./ServicesTable";
import {Typography} from '@material-ui/core';
import {makeStyles} from "@material-ui/core/styles";

const useStyles = makeStyles(theme => ({
  newProfileName: {
    height: 60,
    margin: 0
  },
  profileName: {
    paddingLeft: 10,
    paddingTop: 15,
    height: 60,
    margin: 0
  }
}));

interface ServicesProfileParams {
  newProfile?: boolean,
  profile?: string | undefined,
  setProfile?: (profile: string) => void,
  doesProfileExist?: (profile: string) => boolean,
  services: Array<string>
  addService?: boolean
  deleteIcon?: JSX.Element
  allowEdit?: boolean
  confirmRemove?: boolean
  onServiceAdded?: (service: string) => Promise<boolean>
  onServiceAddCancelled?: () => Promise<boolean>
  onServiceRemoved?: (service: string) => Promise<boolean>
}

const ServicesProfile = (params: ServicesProfileParams) => {
  const { newProfile, profile, setProfile, doesProfileExist, services, addService, deleteIcon, allowEdit, confirmRemove,
    onServiceAdded, onServiceAddCancelled, onServiceRemoved } = params

  const classes = useStyles()

  return (<>
    { newProfile ?
      <TextField  className={classes.newProfileName}
                  autoFocus
                  disabled={!newProfile}
                  error={(newProfile && (!profile || (doesProfileExist?.(profile))))}
                  fullWidth
                  helperText={(newProfile && profile && doesProfileExist?.(profile)) ? 'Profile already exists': ''}
                  label="Profile"
                  margin="normal"
                  onChange={(e: any) => setProfile?.(e.target.value)}
                  required
                  value={profile?profile:''}
                  variant="outlined"
      /> : <Typography className={classes.profileName}
                  variant="h6"
      >Profile '{profile}'
    </Typography>}
    <ServicesTable services={services}
                   addService={addService}
                   deleteIcon={deleteIcon}
                   allowEdit={allowEdit}
                   confirmRemove={confirmRemove}
                   onServiceAdded={onServiceAdded}
                   onServiceAddCancelled={onServiceAddCancelled}
                   onServiceRemoved={onServiceRemoved}
    />
  </>)
}

export default ServicesProfile;
