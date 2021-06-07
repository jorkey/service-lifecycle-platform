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
import {GridActions} from "./GridActions";

const useStyles = makeStyles(theme => ({
  input: {
    width: '100%'
  },
  actionsColumn: {
    width: '200px',
    padding: '4px',
    paddingRight: '40px',
    textAlign: 'right'
  }
}));

export type GridTableColumnValue = string|number|boolean|Date|undefined

export interface GridTableColumnParams {
  name: string,
  headerName: string,
  className: string,
  type?: string,
  select?: string[],
  editable?: boolean,
  validate?: (value: GridTableColumnValue, rowNum: number|undefined) => boolean
}

export interface GridTableRowParams {
  columns: Array<GridTableColumnParams>,
  values: Map<string, GridTableColumnValue>,
  rowNum?: number,
  editable?: boolean,
  adding?: boolean,
  editing?: boolean,
  actions?: JSX.Element[],
  onClick?: () => void,
  onAdd?: (values: Map<string, GridTableColumnValue>) => void,
  onAddCancel?: () => void,
  onEditing?: (editing: boolean) => void,
  onChange?: (oldValues: Map<string, GridTableColumnValue>, newValues: Map<string, GridTableColumnValue>) => void,
  onAction?: (actionIndex: number, values: Map<string, GridTableColumnValue>) => void
}

export const GridTableRow = (params: GridTableRowParams) => {
  const { rowNum, columns, values, editable, adding, editing, actions, onClick, onAdd, onAddCancel, onEditing, onChange, onAction } = params

  const [editColumn, setEditColumn] = useState<string>()
  const [editOldValues, setEditOldValues] = useState<Map<string, GridTableColumnValue>>(new Map())
  const [editValues, setEditValues] = useState<Map<string, GridTableColumnValue>>(new Map())

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
                  } else {
                    onClick?.()
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

  return (<TableRow hover>
      {[...valuesColumns
        , <TableCell key={-1} className={classes.actionsColumn}>
            <GridActions editable={editable}
                         adding={adding}
                         editing={editing}
                         valid={!columns.find(
                             column => { return !column.validate?.(editValues.get(column.name), rowNum) })}
                         actions={actions}
                         onAction={ (actionIndex) => onAction?.(actionIndex, values) }
                         onSubmit={() => {
                             if (adding) {
                               onAdd?.(editValues)
                             } else if (editing) {
                               onChange?.(editOldValues, editValues)
                               onEditing?.(false)
                             }
                             setEditValues(new Map())
                           }}
                         onCancel={() => {
                             if (adding) {
                               onAddCancel?.()
                             } else if (editing) {
                               onEditing?.(false)
                             }
                           }}
        />
        </TableCell>
      ]}
    </TableRow>)
}
