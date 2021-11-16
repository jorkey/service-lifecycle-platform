import React, {useEffect, useRef, useState} from "react";
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
  columnValues: Map<string, GridTableColumnValue>
  checkBoxColumn?: boolean,
  disableManualCheck?: boolean,
}

export interface GridTableRowInternalParams extends GridTableRowParams {
  columns: Array<GridTableColumnParams>,
  rowNum?: number,
  adding?: boolean,
  editing?: boolean,
  changingInProgress?: boolean,
  scrollInto?: boolean,
  onClick?: () => void,
  onBeginEditing?: () => boolean,
  onSubmitted?: (values: Map<string, GridTableColumnValue>, oldValues: Map<string, GridTableColumnValue>) => void,
  onCanceled?: () => void,
  onChecked?: () => void
  onUnchecked?: () => void
}

// @ts-ignore
const useMountEffect = fun => useEffect(fun, [])

export const GridTableRow = (params: GridTableRowInternalParams) => {
  const { rowNum, columns, columnValues, checkBoxColumn, disableManualCheck, adding, editing, scrollInto,
    onClick, onBeginEditing, onSubmitted, onCanceled, onChecked, onUnchecked } = params

  const [editColumn, setEditColumn] = useState<string>()
  const [editOldValues, setEditOldValues] = useState<Map<string, GridTableColumnValue>>(new Map())
  const [editValues, setEditValues] = useState<Map<string, GridTableColumnValue>>(new Map())

  const classes = useStyles()

  const myRef = useRef(null)

  // @ts-ignore
  const executeScroll = () => { if (scrollInto) myRef.current.scrollIntoView() }
  useMountEffect(executeScroll)

  if (!editing && editColumn) {
    setEditColumn(undefined)
    setEditOldValues(new Map())
    setEditValues(new Map())
  }

  const selected = columnValues.get('selected') as boolean

  const valuesColumns =
    columns.map((column, index) =>
      (<TableCell key={index} className={column.className}
                  onClick={() => {
                    if (!adding && column.editable && editColumn != column.name) {
                      setEditColumn(column.name)
                      if (!editing && onBeginEditing?.()) {
                        setEditOldValues(new Map(columnValues))
                        setEditValues(new Map(columnValues))
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
                    checked={adding || editing ? editValues.get(column.name) ? editValues.get(column.name) as boolean : false : columnValues.get(column.name) as boolean}
                    onChange={() => {
                      const value = adding || editing ? editValues.get(column.name) as boolean : columnValues.get(column.name) as boolean
                      setEditValues(editValues => new Map(editValues.set(column.name, !value)))
                    }}
          />
          : column.type == 'date' ?
            columnValues.get(column.name)?
              ((columnValues.get(column.name) as Date).toLocaleString()+'.'+(columnValues.get(column.name) as Date).getMilliseconds()):''
          : column.type == 'elements' ?
              (column.name == 'actions' ?
                <GridActions adding={adding}
                             editing={editing}
                             valid={!columns.find(
                               c => { return c.validate && !c.validate(editValues.get(c.name), rowNum) })}
                             actions={columnValues.get(column.name)! as JSX.Element[]}
                             onSubmit={() => onSubmitted?.(editValues, editOldValues) }
                             onCancel={() => onCanceled?.() }
                />
                : columnValues.get(column.name)! as JSX.Element[])
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
          : columnValues.get(column.name)!
      }
      </TableCell>))

  return (<TableRow ref={myRef} selected={selected}>{ checkBoxColumn ?
            <TableCell padding='checkbox' key={-1}>
              <Checkbox
                checked={selected}
                disabled={disableManualCheck}
                onChange={(event) => {
                  if (event.target.checked) {
                    onChecked?.()
                  } else {
                    onUnchecked?.()
                  }
                }}
              />
            </TableCell>:null}{valuesColumns}
          </TableRow>)
}
