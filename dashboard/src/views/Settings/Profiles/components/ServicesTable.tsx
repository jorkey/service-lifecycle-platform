import React, {useState} from "react";
import {makeStyles} from "@material-ui/core/styles";
import GridTable from "../../../../common/components/gridTable/GridTable";
import ConfirmDialog from "../../../../common/components/dialogs/ConfirmDialog";
import {Button} from "@material-ui/core";
import {GridTableCellParams} from "../../../../common/components/gridTable/GridTableCell";
import {GridTableColumnParams} from "../../../../common/components/gridTable/GridTableColumn";

const useStyles = makeStyles(theme => ({
  servicesTable: {
    marginTop: 20
  },
  serviceColumn: {
  },
  actionsColumn: {
    width: '200px',
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
  onServiceAdded?: (service: string) => Promise<boolean>
  onServiceAddCancelled?: () => void
  onServiceChanged?: (oldServiceName: string, newServiceName: string) => Promise<boolean>
  onServiceRemoved?: (service: string) => Promise<boolean>
}

export const ServicesTable = (props: ServicesTableParams) => {
  const { services, addService, deleteIcon, allowEdit, confirmRemove,
    onServiceAdded, onServiceAddCancelled, onServiceChanged, onServiceRemoved } = props;

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
            return index != rowNum && row.get('service')?.value == value
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

  const rows = new Array<Map<string, GridTableCellParams>>()
  services.forEach(service => {
    rows.push(
      new Map<string, GridTableCellParams>([
        ['service', { value: service }],
        ['actions', { value: deleteIcon ?
            [<Button key={0} onClick={ () => confirmRemove ? setDeleteConfirm(service) : onServiceRemoved?.(service) }>
              {deleteIcon}
            </Button>] : undefined }]
        ]))
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
        return onServiceChanged!(oldValues.get('service')! as string, values.get('service')! as string) }}
    />
    { deleteConfirm ? (
      <ConfirmDialog
        message={`Do you want to delete service '${deleteConfirm}'?`}
        open={true}
        close={() => { setDeleteConfirm(undefined) }}
        onConfirm={() => {
          onServiceRemoved?.(deleteConfirm)
          setDeleteConfirm(undefined)
        }}
      />) : null }
  </>)
}
