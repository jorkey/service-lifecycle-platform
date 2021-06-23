import React, {useState} from "react";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow
} from "@material-ui/core";
import {GridTableColumnParams, GridTableColumnValue} from "./GridTableColumn";
import {GridTableRow} from "./GridTableRow";

interface GridParams {
  className: string,
  columns: GridTableColumnParams[],
  rows: Map<string, GridTableColumnValue>[],
  addNewRow?: boolean,
  onClick?: (row: number, values: Map<string, GridTableColumnValue>) => void,
  onRowAdded?: (values: Map<string, GridTableColumnValue>) => Promise<void> | void,
  onRowAddCancelled?: () => void,
  onChanging?: boolean,
  onRowChanged?: (row: number, values: Map<string, GridTableColumnValue>,
                  oldValues: Map<string, GridTableColumnValue>) => Promise<void> | void
}

export const GridTable = (props: GridParams) => {
  const { className, columns, rows, addNewRow,
    onClick, onRowAdded, onRowAddCancelled, onRowChanged } = props

  const [editingRow, setEditingRow] = useState(-1)
  const [changingInProgress, setChangingInProgress] = useState(false)

  return (
    <Table
      className={className}
      stickyHeader
    >
      <TableHead>
        <TableRow>
          { columns.map((column, index) =>
            <TableCell key={index} className={column.className}>{column.headerName}</TableCell>) }
        </TableRow>
      </TableHead>
      { <TableBody>
          { (addNewRow ?
            (<GridTableRow key={-1} columns={columns} values={new Map()} adding={addNewRow}
                           onSubmitted={(values, oldValues) =>
                             onRowAdded?.(values) }
                           onCanceled={() => onRowAddCancelled?.()}/>) : null) }
          {  rows.map((row, rowNum) => {
              return (<GridTableRow key={rowNum} rowNum={rowNum} columns={columns} values={row}
                                    adding={false}
                                    editing={rowNum == editingRow}
                                    onClick={() => onClick?.(rowNum, row)}
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
    </Table>)
}

export default GridTable;