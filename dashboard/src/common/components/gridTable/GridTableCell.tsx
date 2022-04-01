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
import moment from "moment/moment";

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
  onValidate?: (value: GridTableCellValue) => boolean
  onClicked?: () => void
  onStartEdit?: () => void
  onStopEdit?: () => void
  onSetEditValue?: (value: GridTableCellValue) => void
  onSubmitted?: () => void
  onCancelled?: () => void
  onSelected?: () => void
  onUnselected?: () => void
}

export const GridTableCell = (params: GridTableCellInternalParams) => {
  const { name, className, type, value, select, editable, editing, auto, focused,
    onValidate, onClicked, onStartEdit, onStopEdit, onSetEditValue,
    onSubmitted, onCancelled, onSelected, onUnselected } = params

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
                  checked={value as boolean}
                  disabled={!editable}
                  onChange={(e) => {
                    if (name == 'select') {
                      if (e.target.checked) {
                        onSelected?.()
                      } else {
                        onUnselected?.()
                      }
                    } else {
                      onSetEditValue?.(!value as boolean)
                    }
                  }}
        />
      : type == 'time' ?
        value ? (value as Date).toLocaleString() : ''
      : type == 'relativeTime' ?
        value ? moment(value as Date).fromNow() : ''
      : type == 'upload' ?
          editing ?
            <label htmlFor="upload" style={{ display: 'flex' }}>
              <Typography style={{ paddingTop: '10px' }}>
                {value?(value as File).name:''}
              </Typography>
              <input id="upload"
                     className={classes.input}
                     style={{ display: 'none' }}
                     type='file'
                     onChange={e => onSetEditValue?.(e.target.files![0])}
              />
              <Button
                variant="text"
                style={{ marginLeft: 'auto', marginRight: 0 }}
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
                  value={value?value:''}
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
                 value={value?value:''}
                 autoFocus={focused}
                 onChange={e => onSetEditValue?.(e.target.value)}
                 onKeyPress={e => {
                   const keyCode = e.code || e.key
                   if (keyCode == 'Enter'){
                     onSubmitted?.()
                   }
                 }}
                 error={onValidate?!onValidate(value):false}
          />
      : value!
    }
    </TableCell>)
}
