import React, {useEffect, useRef, useState} from "react";
import {TableCell, TableRow} from "@material-ui/core";
import {GridActions} from "./GridActions";
import {GridTableColumnParams, GridTableCellValue} from "./GridTableColumn";
import {GridTableCell, GridTableCellParams} from "./GridTableCell";

export interface GridTableRowParams {
  columns: Array<GridTableColumnParams>,
  cells: Map<string, GridTableCellParams>,
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
  const { rowNum, columns, cells, adding, editing, scrollInto,
    onClicked, onBeginEditing, onSubmitted, onCanceled, onSelected, onUnselected } = params

  const [editColumn, setEditColumn] = useState<string>()
  const [editOldValues, setEditOldValues] = useState<Map<string, GridTableCellValue>>(new Map())
  const [editValues, setEditValues] = useState<Map<string, GridTableCellValue>>(new Map())
  const editValuesRef = useRef<Map<string, GridTableCellValue>>(new Map())

  const myRef = useRef(null)

  // @ts-ignore
  const executeScroll = () => { if (scrollInto && myRef.current) myRef.current.scrollIntoView(false) }
  useMountEffect(executeScroll)
  executeScroll()

  const modifyEditValues = (values: Map<string, GridTableCellValue>) => {
    setEditValues(values)
    editValuesRef.current = values
  }

  if (!editing && editColumn) {
    setEditColumn(undefined)
    setEditOldValues(new Map())
    modifyEditValues(new Map())
  }

  const valid = !columns.find(c => { return c.validate && !c.validate(editValues.get(c.name), rowNum) })
  const hasGridActions = !!columns.find(c => c.type == 'elements' && c.name == 'actions')

  const valuesColumns = columns.map((column, index) => {
    const columnClassName = cells.get(column.name)?.className
    const cellValue = editing?editValues.get(column.name):cells.get(column.name)?.value
    const cellSelect = cells.get(column.name)?.select
    const editableCell = column.editable ?
      (cells.get(column.name)?.editable != false) : cells.get(column.name)?.editable
    const editingCell = adding || (editing && editColumn === column.name)
    const auto = !!column.auto
    const classNames = column.className && columnClassName ? column.className + ' ' + columnClassName :
      column.className ? column.className : columnClassName ? columnClassName : undefined

    if (column.type == 'elements' && column.name == 'actions') {
      return <TableCell key={index} className={classNames}>
          <GridActions adding={adding}
                       editing={editing}
                       valid={valid}
                       actions={cellValue! as JSX.Element[]}
                       onSubmit={() => onSubmitted?.(editValues, editOldValues)}
                       onCancel={() => onCanceled?.()}
          />
        </TableCell>
    } else {
      return <GridTableCell key={index} name={column.name} className={classNames}
                            type={column.type} value={cellValue} select={cellSelect}
                            editable={editableCell} editing={editingCell} auto={auto}
                            focused={adding && index==0 || editing}
                            onValidate={() => column.validate ? column.validate(editValues.get(column.name), rowNum) : true}
                            onClicked={() => onClicked?.()}
                            onStartEdit={() => {
                              if (onBeginEditing?.()) {
                                setEditColumn(column.name)
                                const values = new Map<string, GridTableCellValue>()
                                editing?editValuesRef.current.forEach((value, key) => values.set(key, value)):
                                  cells.forEach((value, key) => values.set(key, value.value))
                                setEditOldValues(values)
                                modifyEditValues(values)
                              }
                            }}
                            onStopEdit={() => setEditColumn(undefined)}
                            onSetEditValue={(value) => {
                              const newValues = new Map(editValuesRef.current.set(column.name, value))
                              columns.forEach(column => {
                                if (column.auto) {
                                  newValues.set(column.name, column.auto(newValues))
                                }
                              })
                              modifyEditValues(newValues)
                              if (!hasGridActions) {
                                onSubmitted?.(newValues, editOldValues)
                              }
                            }}
                            onCancelled={() => onCanceled?.()}
                            onSelected={() => onSelected?.()}
                            onUnselected={() => onUnselected?.()}
      />
    }
  })

  const selected = !!columns.find(c => { return c.name == 'select' && cells.get(c.name)?.value == true })

  return (<TableRow hover ref={myRef} selected={selected}>{valuesColumns}</TableRow>)
}
