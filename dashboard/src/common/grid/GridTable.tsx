import React, {useState} from "react";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow
} from "@material-ui/core";
import {GridTableColumnParams, GridTableColumnValue, GridTableRow} from "./GridTableRow";
import {makeStyles} from "@material-ui/core/styles";

const useStyles = makeStyles(theme => ({
  actionsColumnHeader: {
    width: '200px',
    padding: '4px',
    paddingRight: '40px',
    textAlign: 'right'
  }
}));

interface GridParams {
  className: string,
  columns: GridTableColumnParams[],
  rows: Map<string, GridTableColumnValue>[],
  editable?: boolean,
  actions?: JSX.Element[],
  addNewRow?: boolean,
  onClick?: (row: number, values: Map<string, GridTableColumnValue>) => void,
  onRowAdded?: (values: Map<string, GridTableColumnValue>) => Promise<void> | void,
  onRowAddCancelled?: () => void,
  onChanging?: boolean,
  onRowChanged?: (row: number, values: Map<string, GridTableColumnValue>,
                  oldValues: Map<string, GridTableColumnValue>) => Promise<void> | void,
  onAction?: (action: number, row: number, values: Map<string, GridTableColumnValue>) => void
}

export const GridTable = (props: GridParams) => {
  const { className, columns, rows, editable, actions, addNewRow,
    onClick, onRowAdded, onRowAddCancelled, onRowChanged, onAction } = props

  const [editingRow, setEditingRow] = useState(-1)
  const [changingInProgress, setChangingInProgress] = useState(false)

  const classes = useStyles()

  return (
    <Table
      className={className}
      stickyHeader
    >
      <TableHead>
        <TableRow>
          { columns.map((column, index) =>
            <TableCell key={index} className={column.className}>{column.headerName}</TableCell>) }
          { (editable || !!actions) ? <TableCell key={-1} className={classes.actionsColumnHeader}>Actions</TableCell> : null }
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
                                    editable={editable}
                                    adding={false}
                                    editing={rowNum == editingRow}
                                    actions={actions}
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
                                    onAction={(index, values) =>
                                      onAction?.(index, rowNum, values) }/>)})}
        </TableBody> }
    </Table>)
}

export default GridTable;