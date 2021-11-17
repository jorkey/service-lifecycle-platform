import React from "react";
import {
  IconButton,
} from "@material-ui/core";
import DoneIcon from "@material-ui/icons/DoneAllTwoTone";
import RevertIcon from "@material-ui/icons/NotInterestedOutlined";

export interface GridActionsParams {
  adding?: boolean,
  editing?: boolean,
  valid?: boolean,
  actions?: JSX.Element[]
  onSubmit?: () => void,
  onCancel?: () => void,
}

export const GridActions = (params: GridActionsParams) => {
  const { adding, editing, valid, actions, onSubmit, onCancel } = params

  return <div>
    { adding ? (<>
      <IconButton
        size={"small"}
        onClick={ () => { onSubmit?.() }}
        title="Done"
        disabled={!valid}
      >
        <DoneIcon/>
      </IconButton>
      <IconButton
        size={"small"}
        onClick={ () => onCancel?.() }
        title="Cancel"
      >
        <RevertIcon/>
      </IconButton>
    </>) : editing ? (<>
      <IconButton
        size={"small"}
        onClick={ () => { onSubmit?.() }}
        title="Done"
        disabled={!valid}
      >
        <DoneIcon/>
      </IconButton>
      <IconButton
        size={"small"}
        onClick={ () => { onCancel?.() }}
        title="Cancel"
      >
        <RevertIcon/>
      </IconButton>
    </>) : actions }
  </div>
}
