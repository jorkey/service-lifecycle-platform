import React, {useState} from "react";
import {
  Checkbox,
  IconButton,
  Input,
  MenuItem,
  Select,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow
} from "@material-ui/core";
import {makeStyles} from "@material-ui/core/styles";
import DeleteIcon from "@material-ui/icons/Delete";
import DoneIcon from "@material-ui/icons/DoneAllTwoTone";
import RevertIcon from "@material-ui/icons/NotInterestedOutlined";

export interface GridActionsParams {
  editable?: boolean,
  adding?: boolean,
  editing?: boolean,
  valid?: boolean,
  actions?: JSX.Element[]
  onAction?: (actionIndex: number) => void
  onSubmit?: () => void,
  onCancel?: () => void,
}

export const GridActions = (params: GridActionsParams) => {
  const { editable, adding, editing, valid, actions, onAction, onSubmit, onCancel } = params

  if (!editable && !adding && editing && !actions) {
    return null
  }

  return <div>
    { adding ? (<>
      <IconButton
        onClick={ () => { onSubmit?.() }}
        title="Done"
        disabled={!valid}
      >
        <DoneIcon/>
      </IconButton>
      <IconButton
        onClick={ () => onCancel?.() }
        title="Cancel"
      >
        <RevertIcon/>
      </IconButton>
    </>) : editing ? (<>
      <IconButton
        onClick={ () => { onSubmit?.() }}
        title="Done"
        disabled={!valid}
      >
        <DoneIcon/>
      </IconButton>
      <IconButton
        onClick={ () => { onCancel?.() }}
        title="Cancel"
      >
        <RevertIcon/>
      </IconButton>
    </>) : actions?.map((element, index) =>
      <IconButton key={index}
        onClick={ () => onAction?.(index) }
      >
        { element }
      </IconButton>
    ) }
  </div>
}
