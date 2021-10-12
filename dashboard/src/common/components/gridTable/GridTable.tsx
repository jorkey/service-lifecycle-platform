import React, {useRef, useState} from "react";
import {
  Checkbox,
  Table,
  TableBody,
  TableCell, TableContainer,
  TableHead,
  TableRow
} from "@material-ui/core";
import {GridTableColumnParams, GridTableColumnValue} from "./GridTableColumn";
import {GridTableRow} from "./GridTableRow";
import {makeStyles} from "@material-ui/styles";
import ReactDOM from "react-dom";

interface GridParams {
  className: string,
  columns: GridTableColumnParams[],
  rows: Map<string, GridTableColumnValue>[],
  checkBoxColumn?: boolean,
  disableManualCheck?: boolean,
  addNewRow?: boolean,
  scrollToLastRow?: boolean,
  onClick?: (row: number) => void,
  onRowAdded?: (values: Map<string, GridTableColumnValue>) => Promise<void> | void,
  onRowAddCancelled?: () => void,
  onChanging?: boolean,
  onRowChanged?: (row: number, values: Map<string, GridTableColumnValue>,
                  oldValues: Map<string, GridTableColumnValue>) => Promise<void> | void,
  onRowsChecked?: (rows: number[]) => void
  onRowsUnchecked?: (rows: number[]) => void
  onScrollTop?: () => void
  onScrollMiddle?: () => void
  onScrollBottom?: () => void
}

export const GridTable = (props: GridParams) => {
  const { className, columns, rows, checkBoxColumn, disableManualCheck, addNewRow, scrollToLastRow,
    onClick, onRowAdded, onRowAddCancelled, onRowChanged, onRowsChecked, onRowsUnchecked,
    onScrollTop, onScrollMiddle, onScrollBottom  } = props

  const [editingRow, setEditingRow] = useState(-1)
  const [changingInProgress, setChangingInProgress] = useState(false)

  const selectedRowsCount = rows.map(row => row.get("selected") as boolean).filter(v => v).length

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
            { checkBoxColumn ?
              <TableCell padding='checkbox'>
                <Checkbox
                  indeterminate={selectedRowsCount > 0 && selectedRowsCount < rows.length}
                  checked={rows.length > 0 && selectedRowsCount === rows.length}
                  disabled={disableManualCheck}
                  onChange={(event) => {
                    if (event.target.checked) {
                      onRowsChecked?.(rows.map((row, index) => index))
                    } else {
                      onRowsUnchecked?.(rows.map((row, index) => index))
                    }
                  }}
                />
              </TableCell> : null}
            { columns.map((column, index) =>
              <TableCell key={index} className={column.className}>{column.headerName}</TableCell>) }
          </TableRow>
        </TableHead>
        { <TableBody onScroll={e => console.log('on scroll ' + e)}>
            { (addNewRow ?
              (<GridTableRow key={-1} columns={columns} values={new Map()} adding={addNewRow}
                             onSubmitted={(values, oldValues) =>
                             onRowAdded?.(values) }
                             onCanceled={() => onRowAddCancelled?.()}/>) : null) }
            {  rows.map((row, rowNum) => {
                return (<GridTableRow key={rowNum} rowNum={rowNum} columns={columns} values={row}
                                      adding={false}
                                      editing={rowNum == editingRow}
                                      checkBoxColumn={checkBoxColumn}
                                      disableManualCheck={disableManualCheck}
                                      scrollInto={
                                        scrollToLastRow && rowNum==rows.length-1
                                      }
                                      onClick={() => {
                                        onClick?.(rowNum)
                                      }}
                                      onChecked={() => {
                                        onRowsChecked?.([rowNum])
                                      }}
                                      onUnchecked={() => {
                                        onRowsUnchecked?.([rowNum])
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