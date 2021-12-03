import React, {useEffect, useRef, useState} from "react";
import {
  Checkbox,
  Input,
  MenuItem,
  Select,
  TableCell,
  TableRow
} from "@material-ui/core";
import {GridActions} from "./GridActions";
import {GridTableColumnParams, GridTableCellValue, GridTableCellParams} from "./GridTableColumn";
import {makeStyles} from "@material-ui/styles";

const useStyles = makeStyles(theme => ({
  editable: {
    border: '1px solid #ccc',
    borderStyle: 'dashed'
  },
  input: {
    width: '100%'
  }
}));

export interface GridTableRowParams {
  columns: Array<GridTableColumnParams>,
  columnValues: Map<string, GridTableCellParams>,
  rowNum?: number,
  adding?: boolean,
  editing?: boolean,
  changingInProgress?: boolean,
  scrollInto?: boolean,
  onClicked?: () => void,
  onBeginEditing?: () => boolean,
  onSubmitted?: (values: Map<string, GridTableCellValue>, oldValues: Map<string, GridTableCellValue>) => void,
  onCanceled?: () => void,
  onSelected?: () => void,
  onUnselected?: () => void
}

// @ts-ignore
const useMountEffect = fun => useEffect(fun, [])

export const GridTableRow = (params: GridTableRowParams) => {
  const { rowNum, columns, columnValues, adding, editing, scrollInto,
    onClicked, onBeginEditing, onSubmitted, onCanceled, onSelected, onUnselected } = params

  const [editColumn, setEditColumn] = useState<string>()
  const [editOldValues, setEditOldValues] = useState<Map<string, GridTableCellValue>>(new Map())
  const [editValues, setEditValues] = useState<Map<string, GridTableCellValue>>(new Map())

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

  const valid = !columns.find(c => { return c.validate && !c.validate(editValues.get(c.name), rowNum) })
  const hasGridActions = !!columns.find(c => c.type == 'elements' && c.name == 'actions')

  const valuesColumns = columns.map((column, index) => {
    const columnClassName = columnValues.get(column.name)?.className
    const cellValue = columnValues.get(column.name)?.value
    const cellSelect = columnValues.get(column.name)?.select
    const editingCell = editing && editColumn === column.name
    const editableCell = column.editable?
      (columnValues.get(column.name)?.editable != false) : columnValues.get(column.name)?.editable
    const editValue = editValues.get(column.name)
    let className = column.className && columnClassName ? column.className + ' ' + columnClassName :
      column.className ? column.className :
        columnClassName ? columnClassName :
          undefined
    if (editableCell && column.type != 'checkbox') {
      className += ' ' + classes.editable
    }

    return (<TableCell key={index} className={className}
                padding={column.type == 'checkbox'?'checkbox':undefined}
                onClick={() => {
                  if (column.name != 'select' && !adding && editableCell && !editingCell) {
                    setEditColumn(column.name)
                    if (!editing && onBeginEditing?.()) {
                      const values = new Map<string, GridTableCellValue>()
                      columnValues.forEach((value, key) => values.set(key, value.value))
                      setEditOldValues(values)
                      setEditValues(values)
                    }
                  } else if (column.type != 'elements') {
                    onClicked?.()
                  }
                }}
                onKeyDown={e => {
                  if (editing && e.keyCode == 27) {
                    onCanceled?.()
                  }
                }}
    > {
      column.type == 'checkbox' ?
        <Checkbox checked={adding || editing ? editValue ? editValue as boolean : false : cellValue as boolean}
                  disabled={!editableCell}
                  onChange={(e) => {
                    if (column.name == 'select') {
                      if (e.target.checked) {
                        onSelected?.()
                      } else {
                        onUnselected?.()
                      }
                    } else {
                      const value = adding || editing ? editValue as boolean : cellValue as boolean
                      setEditValues(editValues => new Map(editValues.set(column.name, !value)))
                      if (!hasGridActions) {
                        onSubmitted?.(editValues, editOldValues)
                      }
                    }
                  }}
        />
      : column.type == 'date' ?
        cellValue?((cellValue as Date).toLocaleString()):''
      : column.type == 'elements' ?
           column.name == 'actions' ?
            <GridActions adding={adding}
                         editing={editing}
                         valid={valid}
                         actions={cellValue! as JSX.Element[]}
                         onSubmit={() => onSubmitted?.(editValues, editOldValues) }
                         onCancel={() => onCanceled?.() }
            />
            : cellValue! as JSX.Element[]
      : adding || editingCell ?
         column.type == 'select' ?
          <Select className={classes.input}
                  autoFocus={true}
                  value={editValues.get(column.name)?editValues.get(column.name):''}
                  open={true}
                  onChange={e => {
                    setEditValues(new Map(editValues.set(column.name, e.target.value as string)))
                    if (!hasGridActions) {
                      onSubmitted?.(editValues, editOldValues)
                    }
                  }}
                  onClose={() => {
                    setEditColumn(undefined)
                  }}
          >
            { cellSelect ? cellSelect?.map(
                ({value, description}, index) =>
                  <MenuItem key={index} value={value}>{description}</MenuItem>) :
              column.select?.map(
                ({value, description}, index) =>
                  <MenuItem key={index} value={value}>{description}</MenuItem>) }
          </Select>
        : <Input className={classes.input}
                 type={column.type}
                 value={editValues.get(column.name)?editValues.get(column.name):''}
                 autoFocus={adding?(index == 0):true}
                 onChange={e => {
                   setEditValues(new Map(editValues.set(column.name, e.target.value)))
                   if (!hasGridActions) {
                     onSubmitted?.(editValues, editOldValues)
                   }
                  }}
                 error={column.validate?!column.validate(editValues.get(column.name), rowNum):false}
          />
      : adding || editing ? editValues.get(column.name)!
      : columnValues.get(column.name)!.value!
    }
    </TableCell>)})

  const selected = !!columns.find(c => { return c.name == 'select' && columnValues.get(c.name)?.value == true })

  return (<TableRow ref={myRef} selected={selected}>{valuesColumns}</TableRow>)
}
