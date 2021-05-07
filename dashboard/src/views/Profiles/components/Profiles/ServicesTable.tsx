import React, {useState} from "react";
import {makeStyles} from "@material-ui/core/styles";
import {DataGrid, GridCellParams, GridColDef, GridRowId, useGridApiRef} from '@material-ui/data-grid';
import {Pagination} from "@material-ui/lab";
import {Button, IconButton} from "@material-ui/core";
import DeleteIcon from '@material-ui/icons/Delete';

const useStyles = makeStyles(theme => ({
  servicesTable: {
    root: {
      '&.MuiDataGrid-root .MuiDataGrid-cell:focus-within': {
        outline: 'none',
      },
    },
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
  newService: boolean
}

export const ServicesTable = (props: ServicesTableParams) => {
  const { profileType, getServices, setServices, newService } = props;
  const apiRef = useGridApiRef();
  const classes = useStyles();

  const columns: GridColDef[] = [
    {
      field: 'service',
      headerName: 'Service',
      flex: 200,
      disableClickEventBubbling: true,
      editable: true
    }]

  const rows = getServices?.().map ((service, index) => {
    return { id: index, service: service, new: false }; }
  )
  if (newService) {
    rows.push({ id: rows.length, service: '', new: true })
  }

  const [selectionModel, setSelectionModel] = React.useState<GridRowId[]>([]);

  return (
    <DataGrid apiRef={apiRef}
              rows={rows} columns={columns}
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
