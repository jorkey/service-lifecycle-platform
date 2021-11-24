import React, {useRef, useState} from "react";
import {
  Checkbox,
  Table,
  TableBody,
  TableCell, TableContainer,
  TableHead,
  TableRow
} from "@material-ui/core";
import {GridTableColumnParams, GridTableCellParams, GridTableCellValue} from "./GridTableColumn";
import {GridTableRow, GridTableRowParams} from "./GridTableRow";

interface GridParams {
  className: string,
  columns: GridTableColumnParams[],
  rows: GridTableRowParams[],
  addNewRow?: boolean,
  scrollToLastRow?: boolean,
  onClick?: (row: number) => void,
  onRowAdded?: (values: Map<string, GridTableCellValue>) => Promise<void> | void,
  onRowAddCancelled?: () => void,
  onChanging?: boolean,
  onRowChanged?: (row: number, values: Map<string, GridTableCellValue>,
                  oldValues: Map<string, GridTableCellValue>) => Promise<void> | void,
  onRowsSelected?: (rows: number[]) => void,
  onRowsUnselected?: (rows: number[]) => void,
  onScrollTop?: () => void,
  onScrollMiddle?: () => void,
  onScrollBottom?: () => void
}

export const GridTable = (props: GridParams) => {
  const { className, columns, rows, addNewRow, scrollToLastRow,
    onClick, onRowAdded, onRowAddCancelled, onRowChanged, onRowsSelected, onRowsUnselected,
    onScrollTop, onScrollMiddle, onScrollBottom  } = props

  const [editingRow, setEditingRow] = useState(-1)
  const [changingInProgress, setChangingInProgress] = useState(false)

  const selectedRowsCount = rows.map(row =>
    row.columnValues.get('select')?.value as boolean).filter(v => v).length

  return (
    <TableContainer className={className}
                    onScroll={e =>  {
                      let element = e.target as HTMLTableElement
                      if (element.scrollTop == 0) {
                        onScrollTop?.()
                      } else if (element.scrollHeight - element.scrollTop === element.clientHeight) {
                        onScrollBottom?.()
                      } else {
                        onScrollMiddle?.()
                      }
                    }}>
      <Table stickyHeader>
        <TableHead>
          <TableRow>
            { columns.map((column, index) => {
              return column.name == 'select' ?
                <TableCell padding='checkbox' className={column.className}>
                  {column.editable == false || rows.find(row => !row.columnValues.get(column.name)?.constant)?
                    <Checkbox className={column.className}
                      indeterminate={selectedRowsCount > 0 && selectedRowsCount < rows.length}
                      checked={rows.length > 0 && selectedRowsCount === rows.length}
                      disabled={column.editable == false ||
                                !rows.find(row => !row.columnValues.get('select')?.constant)}
                      onChange={(event) => {
                        if (event.target.checked) {
                          onRowsSelected?.(rows.map((row, index) => index))
                        } else {
                          onRowsUnselected?.(rows.map((row, index) => index))
                        }
                      }}
                    />:null}
                </TableCell> :
                <TableCell key={index} className={column.className}>
                  {column.headerName}
                </TableCell>
            }) }
          </TableRow>
        </TableHead>
        { <TableBody>
            { (addNewRow ?
              (<GridTableRow key={-1} columns={columns} columnValues={new Map()} adding={addNewRow}
                             onSubmitted={(values, oldValues) =>
                             onRowAdded?.(values) }
                             onCanceled={() => onRowAddCancelled?.()}/>) : null) }
            {  rows.map((row, rowNum) => {
                return (<GridTableRow key={rowNum} rowNum={rowNum} columns={columns}
                                      columnValues={row.columnValues}
                                      adding={false}
                                      editing={rowNum == editingRow}
                                      scrollInto={
                                        scrollToLastRow && rowNum==rows.length-1
                                      }
                                      onClicked={() => {
                                        onClick?.(rowNum)
                                      }}
                                      onSelected={() => {
                                        onRowsSelected?.([rowNum])
                                      }}
                                      onUnselected={() => {
                                        onRowsUnselected?.([rowNum])
                                      }}
                                      onBeginEditing={() => {
                                        if (!changingInProgress) {
                                          setEditingRow(rowNum)
                                          return true
                                        }
                                        return false
                                      }}
                                      onSubmitted={(values, oldValues) => {
                                        const promise = onRowChanged!(rowNum, values, oldValues)
                                        if (promise) {
                                          setChangingInProgress(true)
                                          promise.then(() => {
                                            setEditingRow(-1)
                                            setChangingInProgress(false)
                                          })
                                        } else {
                                          setEditingRow(-1)
                                        }
                                      }}
                                      onCanceled={() => setEditingRow(-1)}
                />)})}
          </TableBody> }
      </Table>
    </TableContainer> )
}

export default GridTable;