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
import {GridTableColumnParams, GridTableColumnValue} from "./GridTableColumn";
import {makeStyles} from "@material-ui/styles";

const useStyles = makeStyles(theme => ({
  input: {
    width: '100%'
  }
}));

export interface GridTableRowParams {
  columnValues: Map<string, GridTableColumnValue>
  constColumns?: string[] | undefined
}

export interface GridTableRowInternalParams extends GridTableRowParams {
  columns: Array<GridTableColumnParams>,
  rowNum?: number,
  adding?: boolean,
  editing?: boolean,
  changingInProgress?: boolean,
  scrollInto?: boolean,
  onClicked?: () => void,
  onBeginEditing?: () => boolean,
  onSubmitted?: (values: Map<string, GridTableColumnValue>, oldValues: Map<string, GridTableColumnValue>) => void,
  onCanceled?: () => void,
  onSelected?: () => void
  onUnselected?: () => void
}

// @ts-ignore
const useMountEffect = fun => useEffect(fun, [])

export const GridTableRow = (params: GridTableRowInternalParams) => {
  const { rowNum, columns, columnValues, constColumns, adding, editing, scrollInto,
    onClicked, onBeginEditing, onSubmitted, onCanceled, onSelected, onUnselected } = params

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

  const valuesColumns =
    columns.map((column, index) =>
      (<TableCell key={index} className={column.className}
                  padding={column.type == 'checkbox'?'checkbox':undefined}
                  onClick={() => {
                    if (!adding && column.editable && editColumn != column.name) {
                      setEditColumn(column.name)
                      if (!editing && onBeginEditing?.()) {
                        setEditOldValues(new Map(columnValues))
                        setEditValues(new Map(columnValues))
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
          (column.editable || !constColumns?.find(c => c == column.name))?
            <Checkbox className={column.className}
                      checked={adding || editing ? editValues.get(column.name) ? editValues.get(column.name) as boolean : false : columnValues.get(column.name) as boolean}
                      disabled={column.editable == false || !!constColumns?.find(c => c == column.name)}
                      onChange={(e) => {
                        if (column.name == 'select')
                          if (e.target.checked) {
                            onSelected?.()
                          } else {
                            onUnselected?.()
                          }
                        else {
                          const value = adding || editing ? editValues.get(column.name) as boolean : columnValues.get(column.name) as boolean
                          setEditValues(editValues => new Map(editValues.set(column.name, !value)))
                        }
                      }}
            />:null
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
              <Select className={column.className}
                      autoFocus={true}
                      value={editValues.get(column.name)?editValues.get(column.name):''}
                      onChange={e => {
                        setEditValues(new Map(editValues.set(column.name, e.target.value as string)))
                      }}
              >
                { column.select.map((item, index) => <MenuItem key={index} value={item}>{item}</MenuItem>) }
              </Select>
            : <Input  className={column.className + "," + classes.input}
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

  const selected = !!columns.find(c => { return c.name == 'select' && columnValues.get(c.name) == true })

  return (<TableRow ref={myRef} selected={selected}>{valuesColumns}</TableRow>)
}
