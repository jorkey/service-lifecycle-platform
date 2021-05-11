import TextField from "@material-ui/core/TextField";
import React, {useState} from "react";
import {ServicesTable, ServiceProfileType} from "./ServicesTable";
import {Typography} from '@material-ui/core';

interface ServicesProfileParams {
  profileType: ServiceProfileType,
  profile?: string | undefined,
  setProfile?: (profile: string) => void,
  doesProfileExist?: (profile: string) => boolean,
  services: Array<string>
  addService?: boolean
  onServiceAdded?: (service: string) => void
  onServiceAddCancelled?: () => void
  onServiceChange?: (oldServiceName: string, newServiceName: string) => void
  onServiceRemove?: (service: string) => void
}

const ServicesProfile = (params: ServicesProfileParams) => {
  const { profileType, profile, setProfile, doesProfileExist, services, addService,
    onServiceAdded, onServiceAddCancelled, onServiceChange, onServiceRemove } = params

  const [ newProfile ] = useState(!profile)

  return (<>
    { newProfile ? <TextField
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
    /> : <Typography variant="h6">Profile {profile}</Typography>}
    <ServicesTable profileType={profileType}
                   services={services}
                   addService={addService}
                   onServiceAdded={onServiceAdded}
                   onServiceAddCancelled={onServiceAddCancelled}
                   onServiceChange={onServiceChange}
                   onServiceRemove={onServiceRemove}
    />
  </>)
}

export default ServicesProfile;
