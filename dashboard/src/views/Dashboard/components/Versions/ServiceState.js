import React, {useState} from "react";
import Popover from "@material-ui/core/Popover";
import {makeStyles} from "@material-ui/styles";
import {Utils} from "../../../../common";
import {Table, TableBody} from "@material-ui/core";
import TableCell from "@material-ui/core/TableCell";
import InfoIcon from "@material-ui/icons/Info";

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

  return state ? (<Popover
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
            state.date?(<TableCell>Date</TableCell><TableCell>{new Date(state.date)}</TableCell>):null
            state.startDate?(<TableCell>Start Date</TableCell><TableCell>{new Date(state.startDate)}</TableCell>):null
            state.updateToVersion?(<TableCell>Updating To</TableCell><TableCell>{new Date(state.updateToVersion)}</TableCell>):null
            state.updateError?(<TableCell>Update Error</TableCell><TableCell>{new Date(state.updateError)}</TableCell>):null
            state.failuresCount?(<TableCell>Failures Count</TableCell><TableCell>{new Date(state.failuresCount)}</TableCell>):null
          </TableBody>
        </Table>
      </Popover>) : null
}
