import React, {useState} from "react";
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

const useStyles = makeStyles((theme:any) => ({
  // selectColumn: {
  //   width: '50px'
  // }
}));

interface GridParams {
  className: string,
  columns: GridTableColumnParams[],
  rows: Map<string, GridTableColumnValue>[],
  select?: boolean,
  selectedRows?: Set<number>,
  addNewRow?: boolean,
  onClick?: (row: number, values: Map<string, GridTableColumnValue>) => void,
  onRowAdded?: (values: Map<string, GridTableColumnValue>) => Promise<void> | void,
  onRowAddCancelled?: () => void,
  onChanging?: boolean,
  onRowChanged?: (row: number, values: Map<string, GridTableColumnValue>,
                  oldValues: Map<string, GridTableColumnValue>) => Promise<void> | void,
  onRowSelected?: (row: number, values: Map<string, GridTableColumnValue>) => void
  onRowUnselected?: (row: number, values: Map<string, GridTableColumnValue>) => void
}

export const GridTable = (props: GridParams) => {
  const { className, columns, rows, select, selectedRows, addNewRow,
    onClick, onRowAdded, onRowAddCancelled, onRowChanged, onRowSelected, onRowUnselected } = props

  const classes = useStyles()

  const [editingRow, setEditingRow] = useState(-1)
  const [changingInProgress, setChangingInProgress] = useState(false)

  return (
    <TableContainer className={className}>
      <Table stickyHeader>
        <TableHead>
          <TableRow>
            { select ?
              <TableCell padding='checkbox'>
                <Checkbox
                  indeterminate={selectedRows && selectedRows.size > 0 && selectedRows.size < rows.length}
                  checked={rows.length > 0 && selectedRows && selectedRows.size === rows.length}
                  onChange={(event) => {
                    if (event.target.checked) {
                      rows.forEach((row, index) => onRowSelected?.(index, row))
                    } else {
                      rows.forEach((row, index) => onRowUnselected?.(index, row))
                    }
                  }}
                />
              </TableCell> : null}
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
                                      select={select}
                                      selected={selectedRows?.has(rowNum)}
                                      onClick={() => {
                                        onClick?.(rowNum, row)
                                      }}
                                      onSelect={() => {
                                        onRowSelected?.(rowNum, row)
                                      }}
                                      onUnselect={() => {
                                        onRowUnselected?.(rowNum, row)
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