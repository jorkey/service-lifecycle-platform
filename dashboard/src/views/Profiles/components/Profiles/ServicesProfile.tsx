import TextField from "@material-ui/core/TextField";
import React, {useState} from "react";
import {ServicesTable, ServiceProfileType} from "./ServicesTable";

interface ServicesProfileParams {
  profileType: ServiceProfileType,
  getProfile?: () => string | undefined,
  setProfile?: (profile: string) => void,
  doesProfileExist?: (profile: string) => boolean,
  getServices: () => Array<string>
  addService?: boolean
  onServiceAdded?: (service: string) => void
  onServiceAddCancelled?: () => void
  onServiceChange?: (oldServiceName: string, newServiceName: string) => void
  onServiceRemove?: (service: string) => void
}

const ServicesProfile = (params: ServicesProfileParams) => {
  const { profileType, getProfile, setProfile, doesProfileExist, getServices, addService,
    onServiceAdded, onServiceAddCancelled, onServiceChange, onServiceRemove } = params

  const [ newProfile ] = useState(!(getProfile?.()))

  const profile = getProfile?.()

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
      value={getProfile?.()?getProfile():''}
      variant="outlined"
    /> : null}
    <ServicesTable profileType={profileType}
                   getServices={getServices}
                   addService={addService}
                   onServiceAdded={onServiceAdded}
                   onServiceAddCancelled={onServiceAddCancelled}
                   onServiceChange={onServiceChange}
                   onServiceRemove={onServiceRemove}
    />
  </>)
}

export default ServicesProfile;
