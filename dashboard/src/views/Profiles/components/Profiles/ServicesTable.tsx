import React, {useState} from "react";
import {makeStyles} from "@material-ui/core/styles";
import {DataGrid, GridCellParams, GridColDef, GridRowId} from '@material-ui/data-grid';
import {Pagination} from "@material-ui/lab";
import {Button, IconButton} from "@material-ui/core";
import DeleteIcon from '@material-ui/icons/Delete';

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
    {
      field: 'service',
      headerName: 'Service',
      flex: 200,
      disableClickEventBubbling: true,
      editable: true
    },
    {
      field: 'actions',
      headerName: 'Actions',
      disableClickEventBubbling: true,
      renderCell: (params: GridCellParams) => (
        <strong>
          <IconButton title='Delete' onClick={() => {}}>
            <DeleteIcon/>
          </IconButton>
        </strong>
      ),
    }]

  const rows = getServices?.().map ((service, index) => {
    return { id: index, service: service }; }
  )

  const [selectionModel, setSelectionModel] = React.useState<GridRowId[]>([]);

  return (
    <DataGrid rows={rows} columns={columns}
              className={classes.servicesTable}
              disableSelectionOnClick={true}
              disableColumnSelector={true}
              disableDensitySelector={true}
              disableMultipleSelection={true}
              onSelectionModelChange={(newSelection) => {
                setSelectionModel([]);
              }}
              selectionModel={selectionModel}
              hideFooter={true}
    />)
}
