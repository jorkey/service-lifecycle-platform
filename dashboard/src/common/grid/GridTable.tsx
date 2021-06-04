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
  onRowAdded?: (values: Map<string, GridTableColumnValue>) => void,
  onRowAddCancelled?: () => void,
  onChanging?: boolean,
  onRowChange?: (row: number, oldValues: Map<string, GridTableColumnValue>, newValues: Map<string, GridTableColumnValue>) => void,
  onAction?: (index: number, row: number, values: Map<string, GridTableColumnValue>) => void
}

export const GridTable = (props: GridParams) => {
  const { className, columns, rows, editable, actions, addNewRow,
    onRowAdded, onRowAddCancelled, onRowChange, onAction } = props

  const [editingRow, setEditingRow] = useState(-1)

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
          { editable ? <TableCell key={-1} className={classes.actionsColumnHeader}>Actions</TableCell> : null }
        </TableRow>
      </TableHead>
      { <TableBody>
          { (addNewRow ?
            (<GridTableRow key={-1} columns={columns} values={new Map()} adding={addNewRow}
                           onAdd={(columns) => onRowAdded?.(columns)}
                           onAddCancel={() => onRowAddCancelled?.()}/>) : null) }
          {  rows.map((row, rowNum) => {
              return (<GridTableRow key={rowNum} rowNum={rowNum} columns={columns} values={row}
                                    editable={editable}
                                    adding={false}
                                    editing={rowNum == editingRow}
                                    actions={actions}
                                    onAdd={onRowAdded}
                                    onAddCancel={() => onRowAddCancelled?.()}
                                    onEditing={(editing) => setEditingRow(editing?rowNum:-1)}
                                    onChange={(oldValues, newValues) => onRowChange?.(
                             rowNum, oldValues, newValues) }
                                    onAction={(index, values) =>
                             onAction?.(index, rowNum, values) }/>) })}
        </TableBody> }
    </Table>)
}

export default GridTable;