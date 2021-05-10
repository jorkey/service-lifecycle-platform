import React, {useState} from "react";
import {IconButton, Input, Table, TableBody, TableCell, TableHead, TableRow} from "@material-ui/core";
import {makeStyles} from "@material-ui/core/styles";
import DeleteIcon from "@material-ui/icons/Delete";
import DoneIcon from "@material-ui/icons/DoneAllTwoTone";
import RevertIcon from "@material-ui/icons/NotInterestedOutlined";

const useStyles = makeStyles(theme => ({
  actionsColumn: {
    width: '200px',
    padding: '4px',
    paddingRight: '40px',
    textAlign: 'right'
  }
}));

export interface EditColumnParams {
  name: string,
  headerName: string,
  className: string,
  editable?: boolean,
  validate?: (value: string) => boolean
}

interface EditTableRowParams {
  columns: Array<EditColumnParams>,
  values: Map<string, string>,
  addRow?: boolean,
  onAdd?: (values: Map<string, string>) => void,
  onAddCancel?: () => void,
  onChange?: (column: string, oldValue: string, newValue: string) => void,
  onRemove?: () => void
}

const EditTableRow = (params: EditTableRowParams) => {
  const { columns, values, addRow, onAdd, onAddCancel, onChange, onRemove } = params

  const [editColumn, setEditColumn] = useState<string>()
  const [editOldValue, setEditOldValue] = useState<string>()
  const [editValues, setEditValues] = useState<Map<string, string>>(new Map())

  const classes = useStyles()

  const valuesColumns = columns.map((column, index) =>
    (<TableCell key={index} className={column.className}
                onClick={() => {
                  if (!addRow && column.editable && editColumn != column.name) {
                    setEditColumn(column.name)
                    setEditOldValue(values.get(column.name)!)
                    setEditValues(new Map([[column.name, values.get(column.name)!]]))
                  }
                }}
                onBlur={() => {
                  if (!addRow && editColumn == column.name && editValues) {
                    onChange?.(column.name, editOldValue!, editValues.get(column.name)!)
                    setEditColumn(undefined)
                    setEditValues(new Map())
                  }
                }}
    >
      {addRow || editColumn === column.name ? (
        <Input
          value={editValues.get(column.name)}
          autoFocus={true}
          onChange={e => {
            setEditValues(new Map(editValues.set(column.name, e.target.value)))
          }}
        />
      ) : (
        values.get(column.name)!
      )}
    </TableCell>))

  console.log('add row ' + addRow)

  return (<TableRow>
      {[...valuesColumns, (
        <TableCell key={valuesColumns.length} className={classes.actionsColumn}>
          { addRow ? (<>
            <IconButton
              onClick={ () => {
                onAdd?.(editValues)
                setEditValues(new Map())
              }}
              title="Cancel"
            >
              <DoneIcon/>
            </IconButton>
            <IconButton
              onClick={ () => addRow?onAddCancel?.():onRemove?.() }
              title="Delete"
            >
              <RevertIcon/>
            </IconButton>
          </>) : (
            <IconButton
              onClick={ () => onRemove?.() }
              title="Delete"
            >
              <DeleteIcon/>
            </IconButton>
          )}
        </TableCell>
      )]}
    </TableRow>)
}

interface EditTableParams {
  className: string,
  columns: Array<EditColumnParams>,
  rows: Array<Map<string, string>>,
  addNewRow?: boolean,
  onRowAdded?: (columns: Map<string, string>) => void,
  onRowAddCancelled?: () => void,
  onRowChange?: (row: number, column: string, oldValue: string, newValue: string) => void,
  onRowRemove?: (row: number) => void
}

export const EditTable = (props: EditTableParams) => {
  const { className, columns, rows, addNewRow, onRowAdded, onRowAddCancelled, onRowChange, onRowRemove } = props
  const classes = useStyles()

  return (
    <Table
      className={className}
      stickyHeader
    >
      <TableHead>
        <TableRow>
          { columns.map((column, index) =>
            <TableCell key={index} className={column.className}>{column.headerName}</TableCell>) }
          <TableCell key={columns.length} className={classes.actionsColumn}>Actions</TableCell>
        </TableRow>
      </TableHead>
      { <TableBody>
          { (addNewRow ?
            (<EditTableRow key={0} columns={columns} values={new Map()} addRow={addNewRow}
                          onAdd={(columns) => onRowAdded?.(columns)}
                          onAddCancel={() => onRowAddCancelled?.()}
                          onChange={(column, value) => {} }
                          onRemove={ () => {} }/>) : null) }
          {  rows.map((row, rowNum) => {
              return (<EditTableRow key={rowNum} columns={columns} values={row} addRow={false}
                           onAdd={() => {}}
                           onAddCancel={() => onRowAddCancelled?.()}
                           onChange={(column, oldValue, newValue) => onRowChange?.(
                             rowNum, column, oldValue, newValue) }
                           onRemove={() => onRowRemove?.(rowNum) }/>) })}
        </TableBody> }
    </Table>)
}

export default EditTable;