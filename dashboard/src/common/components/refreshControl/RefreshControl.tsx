import FormControlLabel from "@material-ui/core/FormControlLabel";
import {Button, InputLabel} from "@material-ui/core";
import RefreshIcon from "@material-ui/icons/Refresh";
import React, {useState} from "react";

interface RefreshControlProps {
  className: string
  refresh: () => void
}

export const RefreshControl = (props: RefreshControlProps) => {
  const { className, refresh } = props

  const [ date, setDate ] = useState(new Date())

  return <FormControlLabel
    className={className}
    label={null}
    control={<Button
      onClick={() => {
        setDate(new Date())
        refresh()
      }}
      title='Refresh'
    >
      <RefreshIcon/>
      <InputLabel>{date.getHours().toLocaleString(undefined, {minimumIntegerDigits: 2}) +
      ':' + date.getMinutes().toLocaleString(undefined, {minimumIntegerDigits: 2}) +
      ':' + date.getSeconds().toLocaleString(undefined, {minimumIntegerDigits: 2})}</InputLabel>
    </Button>}
  />
}