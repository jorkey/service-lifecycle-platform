import React, {useState} from "react";
import {
  Checkbox,
  Input,
  MenuItem,
  Select,
  TableCell,
  TableRow
} from "@material-ui/core";
import {makeStyles} from "@material-ui/core/styles";
import {GridActions} from "./GridActions";
import {GridTableColumnParams, GridTableColumnValue} from "./GridTableColumn";

const useStyles = makeStyles(theme => ({
  input: {
    width: '100%'
  }
}));

export interface GridTableRowParams {
  columns: Array<GridTableColumnParams>,
  values: Map<string, GridTableColumnValue>,
  rowNum?: number,
  adding?: boolean,
  editing?: boolean,
  changingInProgress?: boolean,
  onClick?: () => void,
  onBeginEditing?: () => boolean,
  onSubmitted?: (values: Map<string, GridTableColumnValue>, oldValues: Map<string, GridTableColumnValue>) => void,
  onCanceled?: () => void
}

export const GridTableRow = (params: GridTableRowParams) => {
  const { rowNum, columns, values, adding, editing,
    onClick, onBeginEditing, onSubmitted, onCanceled } = params

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
                    if (!editing && onBeginEditing?.()) {
                      setEditOldValues(new Map(values))
                      setEditValues(new Map(values))
                    }
                  } else if (column.type != 'elements') {
                    onClick?.()
                  }
                }}
                onKeyDown={e => {
                  if (editing && e.keyCode == 27) {
                    onCanceled?.()
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
        />
        : column.type == 'date' ?
          values.get(column.name)?(values.get(column.name) as Date).toLocaleString():''
        : column.type == 'elements' ?
            (column.name == 'actions' ?
              <GridActions adding={adding}
                           editing={editing}
                           valid={!columns.find(
                             c => { return c != column && !c.validate?.(editValues.get(c.name), rowNum) })}
                           actions={values.get(column.name)! as JSX.Element[]}
                           onSubmit={() => onSubmitted?.(editValues, editOldValues) }
                           onCancel={() => onCanceled?.() }
              />
              : values.get(column.name)! as JSX.Element[])
        : (adding || (editing && editColumn === column.name)) ?
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
          : <Input  className={classes.input}
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

  return (<TableRow hover>{valuesColumns}</TableRow>)
}
