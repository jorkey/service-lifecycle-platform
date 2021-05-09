import React, {useState} from "react";
import {makeStyles} from "@material-ui/core/styles";
import EditTable, {EditColumnParams} from "../../../../common/EditTable";
import ConfirmDialog from "../../../../common/ConfirmDialog";

const useStyles = makeStyles(theme => ({
  servicesTable: {
    marginTop: 20
  },
  serviceColumn: {
    width: '200px',
    padding: '4px',
    paddingLeft: '16px'
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
  const { profileType, getServices: getServices, setServices } = props;

  const [ deleteConfirm, setDeleteConfirm ] = useState(false)

  const classes = useStyles();

  const columns: Array<EditColumnParams> = [
    {
      name: 'service',
      headerName: 'Service',
      className: classes.serviceColumn,
      editable: true,
    }
  ]

  const rows = getServices().map(service => { return { service: service }})

  return (<>
    <EditTable
      className={classes.servicesTable}
      columns={columns}
      rows={rows}
      onRowChange={ (row, column, newValue) => {
        setServices?.(getServices().map((value, index) =>
          (index == row)?newValue:value))
      }}
      onRowRemove={ (row) => {
        setServices?.(getServices().filter((value, index) => index != row))
      }}
    />
    {/*<ConfirmDialog*/}
    {/*  message={`Do you want to delete service '${userInfo.user}' (${userInfo.name})?`}*/}
    {/*  open={deleteConfirm}*/}
    {/*  close={() => { setDeleteConfirm(false) }}*/}
    {/*  onConfirm={() => removing(removeUser({ variables: { user: userInfo.user } }).then(() => {}))}*/}
    {/*/>*/}
  </>)
}
