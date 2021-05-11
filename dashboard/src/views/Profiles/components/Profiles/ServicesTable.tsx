import React, {useState} from "react";
import {makeStyles} from "@material-ui/core/styles";
import EditTable, {EditColumnParams} from "../../../../common/EditTable";
import ConfirmDialog from "../../../../common/ConfirmDialog";

const useStyles = makeStyles(theme => ({
  servicesTable: {
    marginTop: 20
  },
  serviceColumn: {
    padding: '4px',
    paddingLeft: '16px'
  }
}));

export enum ServiceProfileType {
  Alone,
  Pattern,
  Projection
}

interface ServicesTableParams {
  profileType: ServiceProfileType
  services: Array<string>
  addService?: boolean
  onServiceAdded?: (service: string) => void
  onServiceAddCancelled?: () => void
  onServiceChange?: (oldServiceName: string, newServiceName: string) => void
  onServiceRemove?: (service: string) => void
}

export const ServicesTable = (props: ServicesTableParams) => {
  const { profileType, services, addService, onServiceAdded, onServiceAddCancelled, onServiceChange, onServiceRemove } = props;

  const [ deleteConfirm, setDeleteConfirm ] = useState<string>()

  const classes = useStyles();

  const columns: Array<EditColumnParams> = [
    {
      name: 'service',
      headerName: 'Service',
      className: classes.serviceColumn,
      editable: true,
      validate: (value) => !!value
    }
  ]

  const rows = new Array<Map<string, string>>()
  services.forEach(service => { rows.push(new Map([['service', service]])) })

  return (<>
    <EditTable
      className={classes.servicesTable}
      columns={columns}
      rows={rows}
      addNewRow={addService}
      onRowAdded={ (columns) => { onServiceAdded?.(columns.get('service')!) } }
      onRowAddCancelled={onServiceAddCancelled}
      onRowChange={ (row, oldValues, newValues) => {
        onServiceChange?.(oldValues.get('service')!, newValues.get('service')!) } }
      onRowRemove={ (row) => {
        setDeleteConfirm(services[row])
      }}
    />
    { deleteConfirm ? (
      <ConfirmDialog
        message={`Do you want to delete service '${deleteConfirm}'?`}
        open={true}
        close={() => { setDeleteConfirm(undefined) }}
        onConfirm={() => onServiceRemove?.(deleteConfirm)}
      />) : null }
  </>)
}
