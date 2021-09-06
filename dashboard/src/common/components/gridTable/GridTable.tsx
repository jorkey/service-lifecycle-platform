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
  selectColumn?: boolean,
  disableManualSelect?: boolean,
  addNewRow?: boolean,
  onClick?: (row: number) => void,
  onRowAdded?: (values: Map<string, GridTableColumnValue>) => Promise<void> | void,
  onRowAddCancelled?: () => void,
  onChanging?: boolean,
  onRowChanged?: (row: number, values: Map<string, GridTableColumnValue>,
                  oldValues: Map<string, GridTableColumnValue>) => Promise<void> | void,
  onRowsSelected?: (rows: number[]) => void
  onRowsUnselected?: (rows: number[]) => void
  onScrollTop?: () => void
  onScrollBottom?: () => void
}

export const GridTable = (props: GridParams) => {
  const { className, columns, rows, selectColumn, disableManualSelect, addNewRow,
    onClick, onRowAdded, onRowAddCancelled, onRowChanged, onRowsSelected, onRowsUnselected,
    onScrollTop, onScrollBottom  } = props

  const [editingRow, setEditingRow] = useState(-1)
  const [changingInProgress, setChangingInProgress] = useState(false)

  const selectedRowsCount = rows.map(row => row.get("selected") as boolean).filter(v => v).length

  return (
    <TableContainer className={className}
                    onScroll={e =>  {
                      let element = e.target as HTMLTableElement
                      if (element.scrollTop == 0) {
                        onScrollTop?.()
                      }
                      if (element.scrollHeight - element.scrollTop === element.clientHeight) {
                        onScrollBottom?.()
                      }
                    }}>
      <Table stickyHeader>
        <TableHead>
          <TableRow>
            { selectColumn ?
              <TableCell padding='checkbox'>
                <Checkbox
                  indeterminate={selectedRowsCount > 0 && selectedRowsCount < rows.length}
                  checked={rows.length > 0 && selectedRowsCount === rows.length}
                  disabled={disableManualSelect}
                  onChange={(event) => {
                    if (event.target.checked) {
                      onRowsSelected?.(rows.map((row, index) => index))
                    } else {
                      onRowsUnselected?.(rows.map((row, index) => index))
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
                                      selectColumn={selectColumn}
                                      disableManualSelect={disableManualSelect}
                                      onClick={() => {
                                        onClick?.(rowNum)
                                      }}
                                      onSelect={() => {
                                        onRowsSelected?.([rowNum])
                                      }}
                                      onUnselect={() => {
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