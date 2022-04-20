import React, {useState} from "react";
import {
  Checkbox,
  Table,
  TableBody,
  TableCell, TableContainer,
  TableHead,
  TableRow
} from "@material-ui/core";
import {GridTableColumnParams, GridTableCellValue} from "./GridTableColumn";
import {GridTableRow} from "./GridTableRow";
import {GridTableCellParams} from "./GridTableCell";

interface GridParams {
  className: string,
  columns: GridTableColumnParams[],
  rows: Map<string, GridTableCellParams>[],
  addNewRow?: boolean,
  scrollToRow?: number,
  onClicked?: (row: number) => void,
  onRowAdded?: (values: Map<string, GridTableCellValue>) => Promise<boolean>,
  onRowAddCancelled?: () => void,
  onChanging?: boolean,
  onRowChanged?: (row: number, values: Map<string, GridTableCellValue>,
                  oldValues: Map<string, GridTableCellValue>) => Promise<boolean>,
  onRowsSelected?: (rows: number[]) => void,
  onRowsUnselected?: (rows: number[]) => void,
  onScrollTop?: () => void,
  onScrollMiddle?: () => void,
  onScrollBottom?: () => void
}

export const GridTable = (props: GridParams) => {
  const { className, columns, rows, addNewRow, scrollToRow,
    onClicked, onRowAdded, onRowAddCancelled, onRowChanged, onRowsSelected, onRowsUnselected,
    onScrollTop, onScrollMiddle, onScrollBottom  } = props

  const [editingRow, setEditingRow] = useState(-1)
  const [changingInProgress, setChangingInProgress] = useState(false)

  const selectedRowsCount = rows.map(row =>
    row.get('select')?.value as boolean).filter(v => v).length

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
                <TableCell key={index} padding='checkbox' className={column.className}>
                  <Checkbox className={column.className}
                    indeterminate={selectedRowsCount > 0 && selectedRowsCount < rows.length}
                    checked={rows.length > 0 && selectedRowsCount === rows.length}
                    disabled={column.editable == false || !rows.find(row => row.get('select')?.editable != false)}
                    onChange={(event) => {
                      if (event.target.checked) {
                        onRowsSelected?.(rows.map((row, index) => index))
                      } else {
                        onRowsUnselected?.(rows.map((row, index) => index))
                      }
                    }}
                  />
                </TableCell> :
                <TableCell key={index} className={column.className}>
                  {column.headerName}
                </TableCell>
            }) }
          </TableRow>
        </TableHead>
        { <TableBody>
            { (addNewRow ?
              (<GridTableRow key={-1} columns={columns} cells={new Map()} adding={addNewRow}
                             onSubmitted={(values, oldValues) =>
                                           onRowAdded?.(values) }
                             onCanceled={() => onRowAddCancelled?.()}/>) : null) }
            {  rows.map((row, rowNum) => {
                return (<GridTableRow key={rowNum} rowNum={rowNum} columns={columns}
                                      cells={row}
                                      adding={false}
                                      editing={rowNum == editingRow}
                                      scrollInto={rowNum == scrollToRow}
                                      onClicked={onClicked?() => {
                                        onClicked(rowNum)
                                      }:undefined}
                                      onSelected={onRowsSelected?() => {
                                        onRowsSelected([rowNum])
                                      }:undefined}
                                      onUnselected={onRowsUnselected?() => {
                                        onRowsUnselected([rowNum])
                                      }:undefined}
                                      onBeginEditing={() => {
                                        if (!changingInProgress) {
                                          setEditingRow(rowNum)
                                          return true
                                        }
                                        return false
                                      }}
                                      onSubmitted={(values, oldValues) => {
                                        const result = onRowChanged!(rowNum, values, oldValues)
                                        setChangingInProgress(true)
                                        result.then((result) => {
                                          if (result) {
                                            setEditingRow(-1)
                                            setChangingInProgress(false)
                                          }
                                        })
                                      }}
                                      onCanceled={() => setEditingRow(-1)}
                />)})}
          </TableBody> }
      </Table>
    </TableContainer> )
}

export default GridTable;