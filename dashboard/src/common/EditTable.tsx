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

export interface EditColumnParams {
  name: string,
  headerName: string,
  className: string,
  type?: string,
  select?: string[],
  editable?: boolean,
  validate?: (value: string | undefined, rowNum: number | undefined) => boolean
}

interface EditTableRowParams {
  columns: Array<EditColumnParams>,
  values: Map<string, string>,
  rowNum?: number,
  adding?: boolean,
  editing?: boolean,
  deleteIcon?: JSX.Element,
  onAdd?: (values: Map<string, string>) => void,
  onAddCancel?: () => void,
  onEditing?: (editing: boolean) => void,
  onChange?: (oldValues: Map<string, string>, newValues: Map<string, string>) => void,
  onRemove?: (values: Map<string, string>) => void
}


const EditTableRow = (params: EditTableRowParams) => {
  const { rowNum, columns, values, adding, editing, deleteIcon, onAdd, onAddCancel, onEditing, onChange, onRemove } = params

  const [editColumn, setEditColumn] = useState<string>()
  const [editOldValues, setEditOldValues] = useState<Map<string, string>>(new Map())
  const [editValues, setEditValues] = useState<Map<string, string>>(new Map())

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
      {adding || (editing && editColumn === column.name) ?
        (column.select ?
          <Select className={classes.input}
                  autoFocus={true}
                  value={editValues.get(column.name)?editValues.get(column.name):''}
                  onChange={e => {
                    setEditValues(new Map(editValues.set(column.name, e.target.value as string)))
                  }}
          >
            { column.select.map((item, index) => <MenuItem key={index} value={item}>{item}</MenuItem>) }
          </Select>
        : column.type == 'checkbox' ?
          <Checkbox className={classes.input}
                    value={editValues.get(column.name)?Boolean(editValues.get(column.name)):false}
                    onChange={e => {
                      setEditValues(new Map(editValues.set(column.name, e.target.value)))
                    }}
          /> :
          <Input  className={classes.input}
                  type={column.type}
                  value={editValues.get(column.name)?editValues.get(column.name):''}
                  autoFocus={adding?(index == 0):true}
                  onChange={e => {
                    setEditValues(new Map(editValues.set(column.name, e.target.value)))
                  }}
                  error={!column.validate?.(editValues.get(column.name), rowNum)}
        />)
      :
        values.get(column.name)!
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

interface EditTableParams {
  className: string,
  columns: Array<EditColumnParams>,
  rows: Array<Map<string, string>>,
  deleteIcon?: JSX.Element,
  addNewRow?: boolean,
  onRowAdded?: (values: Map<string, string>) => void,
  onRowAddCancelled?: () => void,
  onChanging?: boolean,
  onRowChange?: (row: number, oldValues: Map<string, string>, newValues: Map<string, string>) => void,
  onRowRemove?: (row: number, values: Map<string, string>) => void
}

export const EditTable = (props: EditTableParams) => {
  const { className, columns, rows, addNewRow, deleteIcon,
    onRowAdded, onRowAddCancelled, onRowChange, onRowRemove } = props
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
            (<EditTableRow key={-1} columns={columns} values={new Map()} adding={addNewRow}
                           onAdd={(columns) => onRowAdded?.(columns)}
                           onAddCancel={() => onRowAddCancelled?.()}/>) : null) }
          {  rows.map((row, rowNum) => {
              return (<EditTableRow key={rowNum} rowNum={rowNum} columns={columns} values={row} adding={false}
                           editing={rowNum == editingRow}
                           deleteIcon={deleteIcon}
                           onAddCancel={() => onRowAddCancelled?.()}
                           onEditing={(editing) => setEditingRow(editing?rowNum:-1)}
                           onChange={(oldValues, newValues) => onRowChange?.(
                             rowNum, oldValues, newValues) }
                           onRemove={(values) => onRowRemove?.(rowNum, values) }/>) })}
        </TableBody> }
    </Table>)
}

export default EditTable;