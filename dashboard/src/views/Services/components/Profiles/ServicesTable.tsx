import React, {useState} from "react";
import {makeStyles} from "@material-ui/core/styles";
import GridTable from "../../../../common/grid/GridTable";
import ConfirmDialog from "../../../../common/ConfirmDialog";
import {GridTableColumnParams} from "../../../../common/grid/GridTableRow";
import Alert from "@material-ui/lab/Alert";

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

  const columns: Array<GridTableColumnParams> = [
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
    <GridTable
      className={classes.servicesTable}
      columns={columns}
      rows={rows}
      editable={allowEdit}
      actions={deleteIcon?[deleteIcon]:undefined}
      addNewRow={addService}
      onRowAdded={ (columns) => {
        return onServiceAdded!(columns.get('service')! as string) }
      }
      onRowAddCancelled={onServiceAddCancelled}
      onRowChanged={ (row, values, oldValues) => {
        return onServiceChange!(oldValues.get('service')! as string, values.get('service')! as string) }}
      onAction={ (action, row) => {
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
