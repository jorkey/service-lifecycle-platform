import React, {Attributes} from "react";
import {
  Button,
  Checkbox,
  Input,
  MenuItem,
  Select,
  TableCell, Theme, Typography,
} from "@material-ui/core";
import AttachmentIcon from '@material-ui/icons/Attachment';
import {GridColumnType, GridTableCellValue} from "./GridTableColumn";
import {makeStyles} from "@material-ui/styles";

const useStyles = makeStyles<Theme, GridTableCellInternalParams>(theme => ({
  сheckbox: {
  },
  editable: {
    border: '1px solid #ccc',
    borderStyle: 'dashed'
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

export interface GridTableCellInternalParams extends Attributes {
  name: string
  className?: string
  type?: GridColumnType
  value?: GridTableCellValue
  select?: {value:string, description:string}[]
  editable?: boolean
  editing?: boolean
  auto?: boolean
  focused?: boolean
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
  const { name, className, type, value, select, editValue, editable, editing, auto, focused,
    onValidate, onClicked, onStartEdit, onStopEdit, onSetEditValue,
    onCancelled, onSelected, onUnselected } = params

  const classes = useStyles(params)

  let classNames = className
  if (editable && type != 'checkbox') {
    if (classNames) classNames += ' ';
    classNames += classes.editable
  }

  return (
    <TableCell className={classNames}
               padding={type == 'checkbox'?'checkbox':undefined}
               onClick={() => {
                  if (name != 'select' && editable && !editing) {
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
        <Checkbox className={classes.checkbox}
                  checked={editing ? editValue ? editValue as boolean : false : value as boolean}
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
        editing ? editValue ? (editValue as Date).toLocaleString() : '' :
        value ? (value as Date).toLocaleString() : ''
      : type == 'upload' ?
          editing ?
            <label htmlFor="upload" style={{ display: 'flex' }}>
              <Typography style={{ paddingTop: '10px' }}>
                {editValue?(editValue as File).name:''}
              </Typography>
              <input id="upload"
                     className={classes.input}
                     style={{ display: 'none' }}
                     type='file'
                     onChange={e => onSetEditValue?.(e.target.files![0])}
              />
              <Button
                variant="text"
                style={{ textTransform: 'none', marginLeft: 'auto', marginRight: 0 }}
                component="span"
              >
                <AttachmentIcon/>
              </Button>
            </label>
          : value?(value as File).name:''
      : editing && !auto ?
         type == 'select' ?
          <Select className={classes.input}
                  autoFocus={focused}
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
                 autoFocus={focused}
                 onChange={e => onSetEditValue?.(e.target.value)}
                 error={onValidate?!onValidate(editValue):false}
          />
      : editing ? editValue!
      : value!
    }
    </TableCell>)
}
