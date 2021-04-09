import React, {useState} from 'react';
import Popover from '@material-ui/core/Popover';
import {makeStyles} from '@material-ui/styles';
import {Table, TableBody} from '@material-ui/core';
import TableCell from '@material-ui/core/TableCell';
import InfoIcon from '@material-ui/icons/Info';
import AlertIcon from '@material-ui/icons/Error';
import TableRow from '@material-ui/core/TableRow';
import {ServiceState} from '../../../../generated/graphql';
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
    padding: '4px'
  },
  statePopover: {
    pointerEvents: 'none'
  }
}));

interface InfoProps {
  alert: boolean,
  serviceState: ServiceState
}

export const Info: React.FC<InfoProps> = (props) => {
  const { alert, serviceState } = props
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
        state={serviceState}
      />
    </>)
}

interface ServiceStateProps {
  anchor: Element|undefined,
  state: ServiceState
}

export const ServiceStatePopup: React.FC<ServiceStateProps> = (props) => {
  const {anchor, state} = props
  const classes = useStyles()

  return anchor && state &&
      (state.installDate || state.startDate || state.updateToVersion || state.updateError || state.failuresCount) ? (<Popover
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
          {state.installDate?(<TableRow><TableCell className={classes.stateColumn}>Install Date</TableCell>
            <TableCell className={classes.stateColumn}>{new Date(state.installDate).toLocaleString()}</TableCell></TableRow>):null}
          {state.startDate?(<TableRow><TableCell className={classes.stateColumn}>Start Date</TableCell>
            <TableCell className={classes.stateColumn}>{new Date(state.startDate).toLocaleString()}</TableCell></TableRow>):null}
          {state.updateToVersion?(<TableRow><TableCell className={classes.stateColumn}>Updating To</TableCell>
            <TableCell className={classes.stateColumn}>{Version.clientDistributionVersionToString(state.updateToVersion)}</TableCell></TableRow>):null}
          {state.updateError?(<TableRow><TableCell className={classes.stateColumn}>{state.updateError.critical?'Critical Update Error':'Update Error'}</TableCell>
            <TableCell className={classes.stateColumn}>{state.updateError.error}</TableCell></TableRow>):null}
          {state.failuresCount?(<TableRow><TableCell className={classes.stateColumn}>Failures Count</TableCell>
            <TableCell className={classes.stateColumn}>{state.failuresCount}</TableCell></TableRow>):null}
        </TableBody>
      </Table>
    </Popover>) : null
}
