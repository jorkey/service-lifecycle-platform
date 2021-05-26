import React, {useState} from "react";
import {ServicesTable} from "./ServicesTable";
import {Typography} from '@material-ui/core';
import {makeStyles} from "@material-ui/core/styles";

const useStyles = makeStyles(theme => ({
  title: {
    paddingLeft: 10,
    paddingTop: 15,
    height: 60,
    margin: 0
  }
}));

interface DevelopmentServicesParams {
  services: Array<string>
  deleteIcon?: JSX.Element
  onServiceRemove?: (service: string) => void
}

const DevelopmentServices = (params: DevelopmentServicesParams) => {
  const { services, deleteIcon, onServiceRemove } = params

  const classes = useStyles()

  return (<>
    <Typography className={classes.title}
                variant="h6"
      >Not Included Development Services
    </Typography>
    <ServicesTable services={services}
                   deleteIcon={deleteIcon}
                   onServiceRemove={onServiceRemove}
    />
  </>)
}

export default DevelopmentServices;
