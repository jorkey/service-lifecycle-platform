import TextField from "@material-ui/core/TextField";
import React, {useState} from "react";
import {SourcesTable} from "./SourcesTable";
import {Typography} from '@material-ui/core';
import {makeStyles} from "@material-ui/core/styles";

const useStyles = makeStyles(theme => ({
  newServiceName: {
    height: 60,
    margin: 0
  },
  serviceName: {
    paddingLeft: 10,
    paddingTop: 15,
    height: 60,
    margin: 0
  }
}));

interface ServiceSourcesParams {
  newService?: boolean,
  service?: string | undefined,
  setService?: (service: string) => void,
  doesSourceExist?: (service: string) => boolean,
  services: Array<string>
  addSource?: boolean
  deleteIcon?: JSX.Element
  allowEdit?: boolean
  confirmRemove?: boolean
  onSourceAdded?: (service: string) => void
  onSourceAddCancelled?: () => void
  onSourceChange?: (oldServiceName: string, newServiceName: string) => void
  onSourceRemove?: (service: string) => void
}

const ServicesProfile = (params: ServiceSourcesParams) => {
  const { newService, service, setService, doesSourceExist, services, addSource, deleteIcon, allowEdit, confirmRemove,
    onSourceAdded, onSourceAddCancelled, onSourceChange, onSourceRemove } = params

  const classes = useStyles()

  return (<>
    { newService ?
      <TextField  className={classes.newServiceName}
                  autoFocus
                  disabled={!newService}
                  error={(newService && (!service || (doesSourceExist?.(service))))}
                  fullWidth
                  helperText={(newService && service && doesSourceExist?.(service)) ? 'Profile already exists': ''}
                  label="Profile"
                  margin="normal"
                  onChange={(e: any) => setService?.(e.target.value)}
                  required
                  value={service?service:''}
                  variant="outlined"
      /> : <Typography className={classes.serviceName}
                  variant="h6"
      >Profile '{service}'
    </Typography>}
    <ServicesTable services={services}
                   addService={addSource}
                   deleteIcon={deleteIcon}
                   allowEdit={allowEdit}
                   confirmRemove={confirmRemove}
                   onServiceAdded={onSourceAdded}
                   onServiceAddCancelled={onSourceAddCancelled}
                   onServiceChange={onSourceChange}
                   onServiceRemove={onSourceRemove}
    />
  </>)
}

export default ServicesProfile;
