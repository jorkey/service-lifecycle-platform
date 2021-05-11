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
  validate?: (value: string | undefined) => boolean
}

interface EditTableRowParams {
  columns: Array<EditColumnParams>,
  values: Map<string, string>,
  addRow?: boolean,
  onAdd?: (values: Map<string, string>) => void,
  onAddCancel?: () => void,
  onChange?: (oldValues: Map<string, string>, newValues: Map<string, string>) => void,
  onRemove?: () => void
}

const EditTableRow = (params: EditTableRowParams) => {
  const { columns, values, addRow, onAdd, onAddCancel, onChange, onRemove } = params

  const [editing, setEditing] = useState<boolean>()
  const [editColumn, setEditColumn] = useState<string>()
  const [editOldValues, setEditOldValues] = useState<Map<string, string>>(new Map())
  const [editValues, setEditValues] = useState<Map<string, string>>(new Map())

  const classes = useStyles()

  const valuesColumns = columns.map((column, index) =>
    (<TableCell key={index} className={column.className}
                onClick={() => {
                  if (!addRow && column.editable && !editing && editColumn != column.name) {
                    setEditing(true)
                    setEditColumn(column.name)
                    setEditOldValues(new Map(values))
                    setEditValues(new Map(values))
                  }
                }}
    >
      {addRow || (editing && editColumn === column.name) ? (
        <Input
          value={editValues.get(column.name)?editValues.get(column.name):''}
          autoFocus={true}
          onChange={e => {
            setEditValues(new Map(editValues.set(column.name, e.target.value)))
          }}
          error={!column.validate?.(editValues.get(column.name))}
        />
      ) : (
        values.get(column.name)!
      )}
    </TableCell>))

  return (<TableRow>
      {[...valuesColumns, (
        <TableCell key={valuesColumns.length} className={classes.actionsColumn}>
          { addRow ? (<>
            <IconButton
              onClick={ () => {
                onAdd?.(editValues)
                setEditValues(new Map())
              }}
              title="Done"
              disabled={!!columns.find(column => {
                return !column.validate?.(editValues.get(column.name))
              })}
            >
              <DoneIcon/>
            </IconButton>
            <IconButton
              onClick={ () => onAddCancel?.() }
              title="Cancel"
            >
              <RevertIcon/>
            </IconButton>
          </>) : editing ? (<>
            <IconButton
              onClick={ () => {
                onChange?.(editOldValues, editValues)
                setEditing(false)
                setEditColumn(undefined)
                setEditOldValues(new Map())
                setEditValues(new Map())
              }}
              title="Done"
              disabled={!!columns.find(column => {
                return !column.validate?.(editValues.get(column.name))
              })}
            >
              <DoneIcon/>
            </IconButton>
            <IconButton
              onClick={ () => {
                setEditing(false)
                setEditColumn(undefined)
                setEditOldValues(new Map())
                setEditValues(new Map())
              }}
              title="Cancel"
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
  onRowAdded?: (values: Map<string, string>) => void,
  onRowAddCancelled?: () => void,
  onRowChange?: (row: number, oldValues: Map<string, string>, newValues: Map<string, string>) => void,
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
                           onChange={(oldValues, newValues) => onRowChange?.(
                             rowNum, oldValues, newValues) }
                           onRemove={() => onRowRemove?.(rowNum) }/>) })}
        </TableBody> }
    </Table>)
}

export default EditTable;