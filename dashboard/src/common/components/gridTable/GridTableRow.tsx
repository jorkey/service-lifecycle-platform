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

  const valuesColumns =
    columns.map((column, index) => {
      const columnClassName = columnValues.get(column.name)?.className
      const className = column.className && columnClassName ? column.className + ' ' + columnClassName :
                        column.className ? column.className :
                        columnClassName ? columnClassName :
                        undefined
      const constColumn = columnValues.get(column.name)?.constant
      const columnValue = columnValues.get(column.name)?.value
      const editValue = editValues.get(column.name)
      return (<TableCell key={index} className={className}
                  padding={column.type == 'checkbox'?'checkbox':undefined}
                  onClick={() => {
                    if (!adding && column.editable && editColumn != column.name) {
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
      >
        { column.type == 'checkbox' ?
          (column.editable || !constColumn)?
            <Checkbox className={className}
                      checked={adding || editing ? editValue ? editValue as boolean : false : editValue as boolean}
                      disabled={column.editable == false || constColumn}
                      onChange={(e) => {
                        if (column.name == 'select')
                          if (e.target.checked) {
                            onSelected?.()
                          } else {
                            onUnselected?.()
                          }
                        else {
                          const value = adding || editing ? editValue as boolean : columnValue as boolean
                          setEditValues(editValues => new Map(editValues.set(column.name, !value)))
                        }
                      }}
            />:null
          : column.type == 'date' ?
            columnValue?((columnValue as Date).toLocaleString()):''
          : column.type == 'elements' ?
              (column.name == 'actions' ?
                <GridActions adding={adding}
                             editing={editing}
                             valid={!columns.find(
                               c => { return c.validate && !c.validate(editValue, rowNum) })}
                             actions={columnValue! as JSX.Element[]}
                             onSubmit={() => onSubmitted?.(editValues, editOldValues) }
                             onCancel={() => onCanceled?.() }
                />
                : columnValue! as JSX.Element[])
          : (adding || (editing && editColumn === column.name)) ?
            (column.type == 'select' ?
              <Select className={className}
                      autoFocus={true}
                      value={editValues.get(column.name)?editValues.get(column.name):''}
                      onChange={e => {
                        setEditValues(new Map(editValues.set(column.name, e.target.value as string)))
                      }}
              >
                { column.select?.map((item, index) => <MenuItem key={index} value={item}>{item}</MenuItem>) }
              </Select>
            : <Input  className={className + ' ' + classes.input}
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
      </TableCell>)})

  const selected = !!columns.find(c => { return c.name == 'select' && columnValues.get(c.name)?.value == true })

  return (<TableRow ref={myRef} selected={selected}>{valuesColumns}</TableRow>)
}
