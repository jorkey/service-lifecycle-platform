import FormControlLabel from "@material-ui/core/FormControlLabel";
import {Button, InputLabel} from "@material-ui/core";
import RefreshIcon from "@material-ui/icons/Refresh";
import React from "react";

interface RefreshControlProps {
  className: string
  refresh: () => void
}

export const RefreshControl = (props: RefreshControlProps) => {
  const { className, refresh } = props
  return <FormControlLabel
    className={className}
    label={null}
    control={<Button
      onClick={() => refresh()}
      title='Refresh'
    >
      <RefreshIcon/>
      <InputLabel>{new Date().getHours().toLocaleString(undefined, {minimumIntegerDigits: 2}) +
      ':' + new Date().getMinutes().toLocaleString(undefined, {minimumIntegerDigits: 2}) +
      ':' + new Date().getSeconds().toLocaleString(undefined, {minimumIntegerDigits: 2})}</InputLabel>
    </Button>}
  />
}