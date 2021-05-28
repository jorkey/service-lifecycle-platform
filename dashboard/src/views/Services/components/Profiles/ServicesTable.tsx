import React, {useState} from "react";
import {makeStyles} from "@material-ui/core/styles";
import Grid, {GridColumnParams} from "../../../../common/Grid";
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

interface ServicesTableParams {
  services: Array<string>
  addService?: boolean
  deleteIcon?: JSX.Element
  allowEdit?: boolean
  confirmRemove?: boolean
  onServiceAdded?: (service: string) => void
  onServiceAddCancelled?: () => void
  onServiceChange?: (oldServiceName: string, newServiceName: string) => void
  onServiceRemove?: (service: string) => void
}

export const ServicesTable = (props: ServicesTableParams) => {
  const { services, addService, deleteIcon, allowEdit, confirmRemove,
    onServiceAdded, onServiceAddCancelled, onServiceChange, onServiceRemove } = props;

  const [ deleteConfirm, setDeleteConfirm ] = useState<string>()

  const classes = useStyles();

  const columns: Array<GridColumnParams> = [
    {
      name: 'service',
      headerName: 'Service',
      className: classes.serviceColumn,
      editable: allowEdit,
      validate: (value, rowNum) => {
        return !!value &&
          !rows.find((row, index) => {
            return index != rowNum && row.get('service') == value
          })
      }
    }
  ]

  const rows = new Array<Map<string, string>>()
  services.forEach(service => { rows.push(new Map([['service', service]])) })

  return (<>
    <Grid
      className={classes.servicesTable}
      columns={columns}
      rows={rows}
      addNewRow={addService}
      deleteIcon={deleteIcon}
      onRowAdded={ (columns) => { onServiceAdded?.(columns.get('service')! as string) } }
      onRowAddCancelled={onServiceAddCancelled}
      onRowChange={ (row, oldValues, newValues) => {
        onServiceChange?.(oldValues.get('service')! as string, newValues.get('service')! as string) } }
      onRowRemove={ (row) => {
        return confirmRemove ? setDeleteConfirm(services[row]) : onServiceRemove?.(services[row])
      }}
    />
    { deleteConfirm ? (
      <ConfirmDialog
        message={`Do you want to delete service '${deleteConfirm}'?`}
        open={true}
        close={() => { setDeleteConfirm(undefined) }}
        onConfirm={() => {
          onServiceRemove?.(deleteConfirm)
          setDeleteConfirm(undefined)
        }}
      />) : null }
  </>)
}
