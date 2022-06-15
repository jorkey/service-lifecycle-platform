import React, {useState} from 'react';
import Popover from '@material-ui/core/Popover';
import {makeStyles} from '@material-ui/styles';
import {Table, TableBody} from '@material-ui/core';
import TableCell from '@material-ui/core/TableCell';
import InfoIcon from '@material-ui/icons/Info';
import AlertIcon from '@material-ui/icons/Error';
import TableRow from '@material-ui/core/TableRow';
import {InstanceState} from '../../../../generated/graphql';
import {Version} from '../../../../common';

const useStyles = makeStyles(theme => ({
  infoIcon: {
    paddingTop: 5,
    width: 25,
    height: 25,
    opacity: 0.5
  },
  alertIcon: {
    paddingTop: 5,
    width: 25,
    height: 25,
    opacity: 0.5,
    color: 'red'
  },
  stateColumn: {
  },
  statePopover: {
    pointerEvents: 'none'
  }
}));

interface InfoProps {
  alert: boolean,
  instanceState: InstanceState
}

export const Info: React.FC<InfoProps> = (props) => {
  const { alert, instanceState } = props
  const classes = useStyles()

  const [anchor, setAnchor] = React.useState<Element>()

  return (
    <>
      {alert ?
        <AlertIcon
          className={classes.alertIcon}
          onMouseEnter={(event) => setAnchor(event.currentTarget)}
          onMouseLeave={() => setAnchor(undefined)}
        /> :
        <InfoIcon
          className={classes.infoIcon}
          onMouseEnter={(event) => setAnchor(event.currentTarget)}
          onMouseLeave={() => setAnchor(undefined)}
        />}
      <ServiceStatePopup
        anchor={anchor}
        state={instanceState}
      />
    </>)
}

interface ServiceStateProps {
  anchor: Element|undefined,
  state: InstanceState
}

export const ServiceStatePopup: React.FC<ServiceStateProps> = (props) => {
  const {anchor, state} = props
  const classes = useStyles()

  return anchor && state &&
      (state.installTime || state.startTime || state.updateToVersion || state.updateError || state.failuresCount) ? (<Popover
        id="mouse-over-popover"
        className={classes.statePopover}
        open={Boolean(anchor)}
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
          {state.installTime?(<TableRow><TableCell className={classes.stateColumn}>Install Time</TableCell>
            <TableCell className={classes.stateColumn}>{new Date(state.installTime).toLocaleString()}</TableCell></TableRow>):null}
          {state.startTime?(<TableRow><TableCell className={classes.stateColumn}>Start Time</TableCell>
            <TableCell className={classes.stateColumn}>{new Date(state.startTime).toLocaleString()}</TableCell></TableRow>):null}
          {state.updateToVersion?(<TableRow><TableCell className={classes.stateColumn}>Updating To</TableCell>
            <TableCell className={classes.stateColumn}>{Version.clientDistributionVersionToString(state.updateToVersion)}</TableCell></TableRow>):null}
          {state.updateError?(<TableRow><TableCell className={classes.stateColumn}>{state.updateError.critical?'Critical Update Error':'Update Error'}</TableCell>
            <TableCell className={classes.stateColumn}>{state.updateError.error}</TableCell></TableRow>):null}
          {state.failuresCount?(<TableRow><TableCell className={classes.stateColumn}>Faults Count</TableCell>
            <TableCell className={classes.stateColumn}>{state.failuresCount}</TableCell></TableRow>):null}
        </TableBody>
      </Table>
    </Popover>) : null
}
