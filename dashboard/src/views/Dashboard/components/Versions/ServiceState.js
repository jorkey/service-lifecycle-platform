import React, {useState} from "react";
import Popover from "@material-ui/core/Popover";
import {makeStyles} from "@material-ui/styles";
import {Utils} from "../../../../common";
import {Table, TableBody} from "@material-ui/core";
import TableCell from "@material-ui/core/TableCell";
import InfoIcon from "@material-ui/icons/Info";
import TableRow from "@material-ui/core/TableRow";

const useStyles = makeStyles(theme => ({
  infoIcon: {
    paddingTop: 5,
    width: 25,
    height: 25,
    opacity: 0.5
  },
  statePopover: {
    pointerEvents: 'none',
  }
}));

export const Info = (props) => {
  const { client, instance, directory, service } = props

  const classes = useStyles();

  const [anchor, setAnchor] = React.useState()

  return (
    <>
      <InfoIcon
        className={classes.infoIcon}
        onMouseEnter={(event) => setAnchor(event.currentTarget)}
        onMouseLeave={() => setAnchor(null)}
      />
      <ServiceState anchor={anchor} client={client} instance={instance} directory={directory} service={service}/>
    </>)
}

export const ServiceState = (props) => {
  const {anchor, client, instance, directory, service} = props;
  const classes = useStyles();

  const [state, setState] = useState()

  const open = Boolean(anchor)

  if (open) {
    Utils.getServiceState(client, instance, directory, service).then(state => {
      setState(state)
    })
  }

  return state && (state.startDate || state.updateToVersion || state.updateError || state.failuresCount) ? (<Popover
        id="mouse-over-popover"
        className={classes.statePopover}
        classes={{
          paper: classes.paper,
        }}
        open={open}
        anchorEl={anchor}
        anchorOrigin={{
          vertical: 'bottom',
          horizontal: 'left',
        }}
        transformOrigin={{
          vertical: 'top',
          horizontal: 'left',
        }}
        disableRestoreFocus
      >
        <Table>
          <TableBody>
            {state.startDate?(<TableRow><TableCell>Start Date</TableCell><TableCell>{new Date(state.startDate).toLocaleString()}</TableCell></TableRow>):null}
            {state.updateToVersion?(<TableRow><TableCell>Updating To</TableCell><TableCell>{state.updateToVersion}</TableCell></TableRow>):null}
            {state.updateError?(<TableRow><TableCell>Update Error</TableCell><TableCell>{state.updateError}</TableCell></TableRow>):null}
            {state.failuresCount?(<TableRow><TableCell>Failures Count</TableCell><TableCell>{state.failuresCount}</TableCell></TableRow>):null}
          </TableBody>
        </Table>
      </Popover>) : null
}
