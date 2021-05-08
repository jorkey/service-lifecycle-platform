import React, {useState} from "react";
import {IconButton, Input, Table, TableBody, TableCell, TableHead, TableRow} from "@material-ui/core";
import {makeStyles} from "@material-ui/core/styles";

const useStyles = makeStyles(theme => ({
  actionsColumn: {
    width: '200px',
    padding: '4px',
    paddingRight: '40px',
    textAlign: 'right'
  }
}));

export interface EditColumnParams {
  name: string,
  headerName: string,
  className: string,
  editable?: boolean,
  onChange?: (row: number, value: string) => void,
  validate?: (value: string) => boolean
}

interface EditTableRowParams {
  columns: Array<EditColumnParams>,
  rowNum: number,
  values: any,
  onChange: (column: string, value: string) => void,
  onRemove: () => void
}

const EditTableRow = (params: EditTableRowParams) => {
  const { columns, rowNum, values, onRemove } = params
  const [editColumn, setEditColumn] = useState(-1)
  const classes = useStyles()

  const valuesColumns = columns.map((column, index) =>
    (<TableCell className={column.className}
               onDoubleClick={() => {
                 if (column.editable && editColumn != index) {
                   setEditColumn(index)
                 }
               }}
               onBlur={() => setEditColumn(-1)}
    >
      {editColumn == index ? (
        <Input
          value={values[column.name]}
          onChange={e => column.onChange?.(rowNum, e.target.value)}
        />
      ) : (
        values[column.name]
      )}
    </TableCell>))
  return (<>
      {[...valuesColumns, (<TableCell className={classes.actionsColumn}>
             <IconButton
               onClick={() => onRemove()}
               title="Delete"
             />
           </TableCell>
         )]}
    </>)
}

interface EditTableParams {
  className: string,
  columns: Array<EditColumnParams>,
  rows: Array<any>,
  onRowChange: (row: number, column: string, value: string) => void,
  onRowRemove: (row: number) => void
}

export const EditTable = (props: EditTableParams) => {
  const { className, columns, rows, onRowChange, onRowRemove } = props;

  return (
    <Table
      className={className}
      stickyHeader
    >
      <TableHead>
        <TableRow>
          { columns.map(column =>
            <TableCell className={column.className}>{column.name}</TableCell>) }
        </TableRow>
      </TableHead>
      { <TableBody>
          { rows.map((row, index) =>
            (<EditTableRow columns={columns} rowNum={index} values={row}
                           onChange={(column, value) => onRowChange(row, column, value) }
                           onRemove={() => onRowRemove(row) }/>)) }
        </TableBody> }
    </Table>)
}

export default EditTable;