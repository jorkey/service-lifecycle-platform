import React, {useState} from "react";
import {
  Checkbox,
  IconButton,
  Input,
  MenuItem,
  Select,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow
} from "@material-ui/core";
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
  },
  input: {
    width: '100%'
  }
}));

export type GridColumnValue = string|number|boolean|Date|undefined

export interface GridColumnParams {
  name: string,
  headerName: string,
  className: string,
  type?: string,
  select?: string[],
  editable?: boolean,
  validate?: (value: GridColumnValue, rowNum: number|undefined) => boolean
}

interface GridRowParams {
  columns: Array<GridColumnParams>,
  values: Map<string, GridColumnValue>,
  rowNum?: number,
  adding?: boolean,
  editing?: boolean,
  actions?: JSX.Element[],
  onAdd?: (values: Map<string, GridColumnValue>) => void,
  onAddCancel?: () => void,
  onEditing?: (editing: boolean) => void,
  onChange?: (oldValues: Map<string, GridColumnValue>, newValues: Map<string, GridColumnValue>) => void,
  onAction?: (actionIndex: number, values: Map<string, GridColumnValue>) => void
}

const GridRow = (params: GridRowParams) => {
  const { rowNum, columns, values, adding, editing, actions, onAdd, onAddCancel, onEditing, onChange, onAction } = params

  const [editColumn, setEditColumn] = useState<string>()
  const [editOldValues, setEditOldValues] = useState<Map<string, GridColumnValue>>(new Map())
  const [editValues, setEditValues] = useState<Map<string, GridColumnValue>>(new Map())

  const classes = useStyles()

  if (!editing && editColumn) {
    setEditColumn(undefined)
    setEditOldValues(new Map())
    setEditValues(new Map())
  }

  const valuesColumns = columns.map((column, index) =>
    (<TableCell key={index} className={column.className}
                onClick={() => {
                  if (!adding && column.editable && editColumn != column.name) {
                    setEditColumn(column.name)
                    setEditOldValues(new Map(values))
                    setEditValues(new Map(values))
                    onEditing?.(true)
                  }
                }}
                onKeyDown={e => {
                  if (editing && e.keyCode == 27) {
                    onEditing?.(false)
                  }
                }}
    >
      { column.type == 'checkbox' ?
        <Checkbox className={classes.input}
                  checked={adding || editing ? editValues.get(column.name) ? editValues.get(column.name) as boolean : false : values.get(column.name) as boolean}
                  onChange={() => {
                    const value = adding || editing ? editValues.get(column.name) as boolean : values.get(column.name) as boolean
                    setEditValues(editValues => new Map(editValues.set(column.name, !value)))
                  }}
        /> :
        (adding || (editing && editColumn === column.name)) ?
          (column.select ?
            <Select className={classes.input}
                    autoFocus={true}
                    value={editValues.get(column.name)?editValues.get(column.name):''}
                    onChange={e => {
                      setEditValues(new Map(editValues.set(column.name, e.target.value as string)))
                    }}
            >
              { column.select.map((item, index) => <MenuItem key={index} value={item}>{item}</MenuItem>) }
            </Select> :
            <Input  className={classes.input}
                    type={column.type}
                    value={editValues.get(column.name)?editValues.get(column.name):''}
                    autoFocus={adding?(index == 0):true}
                    onChange={e => {
                      setEditValues(new Map(editValues.set(column.name, e.target.value)))
                    }}
                    error={(column.validate)?!column.validate(editValues.get(column.name), rowNum):false}
            />)
      : adding || editing ? editValues.get(column.name)!
      : values.get(column.name)!
    }
    </TableCell>))

  return (<TableRow>
      {[...valuesColumns, (
        <TableCell key={valuesColumns.length} className={classes.actionsColumn}>
          { adding ? (<>
            <IconButton
              onClick={ () => {
                onAdd?.(editValues)
                setEditValues(new Map())
              }}
              title="Done"
              disabled={!!columns.find(column => {
                return !column.validate?.(editValues.get(column.name), rowNum)
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
                onEditing?.(false)
              }}
              title="Done"
              disabled={!!columns.find(column => {
                return !column.validate?.(editValues.get(column.name), rowNum)
              })}
            >
              <DoneIcon/>
            </IconButton>
            <IconButton
              onClick={ () => {
                onEditing?.(false)
              }}
              title="Cancel"
            >
              <RevertIcon/>
            </IconButton>
          </>) : (
            <IconButton
              onClick={ () =>
                onRemove?.(values)
              }
              title="Delete"
            >
              { deleteIcon?deleteIcon:<DeleteIcon/> }
            </IconButton>
          )}
        </TableCell>
      )]}
    </TableRow>)
}

interface GridParams {
  className: string,
  columns: GridColumnParams[],
  rows: Map<string, GridColumnValue>[],
  actions?: JSX.Element[],
  addNewRow?: boolean,
  onRowAdded?: (values: Map<string, GridColumnValue>) => void,
  onRowAddCancelled?: () => void,
  onChanging?: boolean,
  onRowChange?: (row: number, oldValues: Map<string, GridColumnValue>, newValues: Map<string, GridColumnValue>) => void,
  onAction?: (actionIndex: number, row: number, values: Map<string, GridColumnValue>) => void
}

export const Grid = (props: GridParams) => {
  const { className, columns, rows, actions, addNewRow,
    onRowAdded, onRowAddCancelled, onRowChange, onAction } = props
  const classes = useStyles()

  const [editingRow, setEditingRow] = useState(-1)

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
            (<GridRow key={-1} columns={columns} values={new Map()} adding={addNewRow}
                           onAdd={(columns) => onRowAdded?.(columns)}
                           onAddCancel={() => onRowAddCancelled?.()}/>) : null) }
          {  rows.map((row, rowNum) => {
              return (<GridRow key={rowNum} rowNum={rowNum} columns={columns} values={row} adding={false}
                           editing={rowNum == editingRow}
                           deleteIcon={removeRows}
                           onAddCancel={() => onRowAddCancelled?.()}
                           onEditing={(editing) => setEditingRow(editing?rowNum:-1)}
                           onChange={(oldValues, newValues) => onRowChange?.(
                             rowNum, oldValues, newValues) }
                           onRemove={(values) => onRowRemove?.(rowNum, values) }/>) })}
        </TableBody> }
    </Table>)
}

export default Grid;