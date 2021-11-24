import React, {useState} from "react";
import {makeStyles} from "@material-ui/core/styles";
import GridTable from "../../../../common/components/gridTable/GridTable";
import ConfirmDialog from "../../../../common/components/dialogs/ConfirmDialog";
import {GridTableColumnParams, GridTableCellParams} from "../../../../common/components/gridTable/GridTableColumn";
import {Button} from "@material-ui/core";
import {GridTableRowParams} from "../../../../common/components/gridTable/GridTableRow";

const useStyles = makeStyles(theme => ({
  servicesTable: {
    marginTop: 20
  },
  serviceColumn: {
    padding: '4px',
    paddingLeft: '16px'
  },
  actionsColumn: {
    width: '200px',
    padding: '4px',
    paddingRight: '40px',
    textAlign: 'right'
  },
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
            return index != rowNum && row.columnValues.get('service') == value
          })
      }
    },
    {
      name: 'actions',
      headerName: 'Actions',
      type: 'elements',
      className: classes.actionsColumn
    }
  ]

  const rows = new Array<GridTableRowParams>()
  services.forEach(service => {
    rows.push({
      columnValues: new Map<string, GridTableCellParams>([
        ['service', service],
        ['actions', deleteIcon ?
            [<Button onClick={ () => confirmRemove ? setDeleteConfirm(service) : onServiceRemove?.(service) }>
              deleteIcon
            </Button>] : undefined]
      ])})
  })

  return (<>
    <GridTable
      className={classes.servicesTable}
      columns={columns}
      rows={rows}
      addNewRow={addService}
      onRowAdded={ (columns) => {
        return onServiceAdded!(columns.get('service')! as string) }
      }
      onRowAddCancelled={onServiceAddCancelled}
      onRowChanged={ (row, values, oldValues) => {
        return onServiceChange!(oldValues.get('service')! as string, values.get('service')! as string) }}
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
