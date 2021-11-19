import {Button, Dialog, DialogActions, DialogTitle} from "@material-ui/core";
import React from "react";

interface ConfirmDialogParams {
  message: string,
  open: boolean,
  close: () => void,
  onConfirm: () => void,
}

const ConfirmDialog: React.FC<ConfirmDialogParams> = props => {
  const { message, open, close, onConfirm } = props

  const choice: (confirm: boolean) => void = confirm => {
    close()
    if (confirm) {
      onConfirm()
    }
  }

  return (
    <Dialog
      open={open}
      onClose={close}
    >
      <DialogTitle>{message}</DialogTitle>
      <DialogActions>
        <Button onClick={() => choice(false)} color="primary" autoFocus>
          Cancel
        </Button>
        <Button onClick={() => choice(true)} color="primary">
          Confirm
        </Button>
      </DialogActions>
    </Dialog>
  );
}

export default ConfirmDialog