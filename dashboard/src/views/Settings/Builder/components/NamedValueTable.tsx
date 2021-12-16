import React, {useState} from "react";
import {makeStyles} from "@material-ui/core/styles";
import GridTable from "../../../../common/components/gridTable/GridTable";
import ConfirmDialog from "../../../../common/components/dialogs/ConfirmDialog";
import {GridTableColumnParams} from "../../../../common/components/gridTable/GridTableColumn";
import {GridTableCellParams} from "../../../../common/components/gridTable/GridTableCell";
import {NamedStringValue} from "../../../../generated/graphql";
import {Button} from "@material-ui/core";
import DeleteIcon from "@material-ui/icons/Delete";

const useStyles = makeStyles(theme => ({
  table: {
    marginTop: 20
  },
  nameColumn: {
    width: '200px',
  },
  valueColumn: {
  },
  actionsColumn: {
    width: '200px',
    paddingRight: '40px',
    textAlign: 'right'
  },
}));

interface NamedValueTableParams {
  values: Array<NamedStringValue>
  addValue?: boolean
  confirmRemove?: boolean
  onValueAdded?: (value: NamedStringValue) => void
  onValueAddCancelled?: () => void
  onValueChanged?: (oldValue: NamedStringValue, newValue: NamedStringValue) => void
  onValueRemoved?: (value: NamedStringValue) => void
}

const NamedValueTable = (props: NamedValueTableParams) => {
  const { values, addValue, confirmRemove,
    onValueAdded, onValueAddCancelled, onValueChanged, onValueRemoved } = props;

  const [ deleteConfirm, setDeleteConfirm ] = useState<NamedStringValue>()

  const classes = useStyles();

  const columns: GridTableColumnParams[] = [
    {
      name: 'name',
      headerName: 'Name',
      className: classes.nameColumn,
      editable: true,
      validate: (value, rowNum) => {
        return !!value &&
          !rows.find((row, index) => {
            return index != rowNum && row.get('name')?.value == value
          })
      }
    },
    {
      name: 'value',
      headerName: 'Value',
      className: classes.valueColumn,
      editable: true,
      validate: (value, rowNum) => {
        try {
          return value?!!new URL(value as string):false
        } catch (ex) {
          return false
        }
      }
    },
  ]

  const rows = values.map(value => (
    new Map<string, GridTableCellParams>([
      ['name', { value: value.name }],
      ['value', { value: value.value }],
      ['actions', { value: [<Button key='0' onClick={ () => confirmRemove ? setDeleteConfirm(value) : onValueRemoved?.(value) }>
          <DeleteIcon/>
        </Button>] }]
    ])))

  return (<>
    <GridTable
      className={classes.table}
      columns={columns}
      rows={rows}
      addNewRow={addValue}
      onRowAdded={ (cells) => {
        onValueAdded?.({ name: cells.get('name')! as string,
          value: cells.get('value')! as string }) }}
      onRowAddCancelled={onValueAddCancelled}
      onRowChanged={ (row, cells, oldValues) => {
        onValueChanged!(values[row], { name: cells.get('name')! as string,
          value: cells.get('value')! as string }) }}
    />
    { deleteConfirm ? (
      <ConfirmDialog
        message={`Do you want to delete value '${deleteConfirm.name}'?`}
        open={true}
        close={() => { setDeleteConfirm(undefined) }}
        onConfirm={() => {
          onValueRemoved?.(deleteConfirm)
          setDeleteConfirm(undefined)
        }}
      />) : null }
  </>)
}

export default NamedValueTable;
