import TextField from "@material-ui/core/TextField";
import React, {useState} from "react";
import {SourcesTable} from "./SourcesTable";
import {Typography} from '@material-ui/core';
import {makeStyles} from "@material-ui/core/styles";
import {SourceConfig} from "../../../../generated/graphql";

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
  doesServiceExist?: (service: string) => boolean,
  sources: Array<SourceConfig>
  addSource?: boolean
  confirmRemove?: boolean
  onSourceAdded?: (source: SourceConfig) => void
  onSourceAddCancelled?: () => void
  onSourceChange?: (oldSource: SourceConfig, newSource: SourceConfig) => void
  onSourceRemove?: (source: SourceConfig) => void
}

const ServiceSources = (params: ServiceSourcesParams) => {
  const { newService, service, setService, doesServiceExist, sources, addSource, confirmRemove,
    onSourceAdded, onSourceAddCancelled, onSourceChange, onSourceRemove } = params

  const classes = useStyles()

  return (<>
    { newService ?
      <TextField  className={classes.newServiceName}
                  autoFocus
                  disabled={!newService}
                  error={(newService && (!service || (doesServiceExist?.(service))))}
                  fullWidth
                  helperText={(newService && service && doesServiceExist?.(service)) ? 'Profile already exists': ''}
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
    <SourcesTable sources={sources}
                  addSource={addSource}
                  confirmRemove={confirmRemove}
                  onSourceAdded={onSourceAdded}
                  onSourceAddCancelled={onSourceAddCancelled}
                  onSourceChange={onSourceChange}
                  onSourceRemove={onSourceRemove}
    />
  </>)
}

export default ServiceSources;
