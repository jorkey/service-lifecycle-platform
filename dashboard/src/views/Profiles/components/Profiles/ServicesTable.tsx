import React, {useState} from "react";
import {makeStyles} from "@material-ui/core/styles";
import {DataGrid, GridColDef} from '@material-ui/data-grid';

const useStyles = makeStyles(theme => ({
  servicesTable: {
    color: "black",
    marginTop: 20,
    height: 400,
    fontWeight: 200
  },
  controls: {
    marginTop: 25,
    display: 'flex',
    justifyContent: 'flex-end',
    p: 2
  },
  control: {
    marginLeft: '25px'
  }
}));

export enum ServiceProfileType {
  Alone,
  Pattern,
  Projection
}

interface ServicesTableParams {
  profileType: ServiceProfileType
  getServices: () => Array<string>
  setServices?: (services: Array<string>) => any
}

export const ServicesTable = (props: ServicesTableParams) => {
  const { profileType, getServices, setServices } = props;
  const classes = useStyles();

  const columns: GridColDef[] = [
    { field: 'service', headerName: 'Service', flex: 200 } ]

  const rows = getServices?.().map ((service, index) => {
    return { id: index, service: service }; }
  )

  return (
    <DataGrid rows={rows} columns={columns} className={classes.servicesTable}/>)
}
