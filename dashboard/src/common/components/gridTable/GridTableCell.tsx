import React from "react";
import {
  Checkbox,
  Input,
  MenuItem,
  Select,
  TableCell, Theme,
} from "@material-ui/core";
import {GridColumnType, GridTableCellValue} from "./GridTableColumn";
import {makeStyles} from "@material-ui/styles";

const useStyles = makeStyles<Theme, GridTableCellInternalParams>(theme => ({
  editable: {
    border: '1px solid #ccc',
    borderStyle: 'dashed'
  },
  width: {
    minWidth: props => props.width,
    maxWidth: props => props.width
  },
  input: {
    width: '100%'
  }
}));

export interface GridTableCellParams {
  value: GridTableCellValue
  className?: string
  editable?: boolean
  select?: {value:string, description:string}[]
}

export interface GridTableCellInternalParams {
  index: number
  name: string,
  className?: string
  width?: number
  type?: GridColumnType
  value?: GridTableCellValue
  select?: {value:string, description:string}[]
  editable?: boolean
  editing?: boolean
  editValue?: GridTableCellValue
  onValidate?: (value: GridTableCellValue) => boolean
  onClicked?: () => void
  onStartEdit?: () => void
  onStopEdit?: () => void
  onSetEditValue?: (value: GridTableCellValue) => void
  onCancelled?: () => void
  onSelected?: () => void
  onUnselected?: () => void
}

export const GridTableCell = (params: GridTableCellInternalParams) => {
  const { index, name, className, width, type, value, select, editValue, editing, editable,
    onValidate, onClicked, onStartEdit, onStopEdit, onSetEditValue,
    onCancelled, onSelected, onUnselected } = params

  const classes = useStyles(params)

  let classNames = className
  if (width) {
    if (classNames) classNames += ' ';
    classNames += classes.width
  }
  if (editable && type != 'checkbox') {
    if (classNames) classNames += ' ';
    classNames += classes.editable
  }

  return (
    <TableCell key={index} className={classNames}
        padding={type == 'checkbox'?'checkbox':undefined}
        onClick={() => {
          if (editable && !editing) {
            onStartEdit?.()
          } else if (type != 'elements') {
            onClicked?.()
          }
        }}
        onKeyDown={e => {
          if (editing && e.keyCode == 27) {
            onCancelled?.()
          }
        }}
    > {
      type == 'checkbox' ?
        <Checkbox checked={editing ? editValue ? editValue as boolean : false : value as boolean}
                  disabled={!editable}
                  onChange={(e) => {
                    if (name == 'select') {
                      if (e.target.checked) {
                        onSelected?.()
                      } else {
                        onUnselected?.()
                      }
                    } else {
                      const v = editing ? editValue as boolean : value as boolean
                      onSetEditValue?.(!v)
                    }
                  }}
        />
      : type == 'date' ?
        value?((value as Date).toLocaleString()):''
      : editing ?
         type == 'select' ?
          <Select className={classes.input}
                  autoFocus={true}
                  value={editValue?editValue:''}
                  open={true}
                  onChange={e => onSetEditValue?.(e.target.value as string)}
                  onClose={() => onStopEdit?.()}
          >
            { select?.map(
                ({value, description}, index) =>
                  <MenuItem key={index} value={value}>{description}</MenuItem>) }
          </Select>
        : <Input className={classes.input}
                 type={type}
                 value={editValue?editValue:''}
                 autoFocus={index == 0}
                 onChange={e => onSetEditValue?.(e.target.value)}
                 error={onValidate?!onValidate(editValue):false}
          />
      : editing ? editValue!
      : value!
    }
    </TableCell>)
}
