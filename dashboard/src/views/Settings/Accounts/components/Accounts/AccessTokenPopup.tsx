import React, {useState} from 'react';
import Popover from '@material-ui/core/Popover';
import {makeStyles} from '@material-ui/styles';
import {Button, Grid, Table, TableBody, Typography} from '@material-ui/core';
import TableCell from '@material-ui/core/TableCell';
import InfoIcon from '@material-ui/icons/Info';
import AlertIcon from '@material-ui/icons/Error';
import TableRow from '@material-ui/core/TableRow';
import {ServiceState, useAccessTokenQuery} from '../../../../../generated/graphql';
import {Version} from '../../../../../common';
import AccessTokenIcon from "@material-ui/icons/VpnKey";

const useStyles = makeStyles(theme => ({
  title: {
    paddingLeft: '12px'
  },
  token: {
    paddingRight: '12px'
  }
}));

interface AccessTokenPopupProps {
  account: string
}

export const AccessTokenPopup: React.FC<AccessTokenPopupProps> = (props) => {
  const {account} = props
  const classes = useStyles()

  const [popupAnchor, setPopupAnchor] = React.useState<Element>()

  const { data:accessToken } = useAccessTokenQuery({
    fetchPolicy: 'no-cache', // base option no-cache does not work
    variables: { account: account }
  })

  return <>
    <Button onClick={
      (event) => {
        setPopupAnchor(event.currentTarget)
      } }>
      <AccessTokenIcon/>
    </Button>
      { accessToken?.accessToken?<Popover
          id="mouse-over-popover"
          open={Boolean(popupAnchor)}
          anchorEl={popupAnchor}
          anchorOrigin={{
            vertical: 'bottom',
            horizontal: 'left',
          }}
          transformOrigin={{
            vertical: 'top',
            horizontal: 'left',
          }}
          onClose={() => setPopupAnchor(undefined)}
      >
        <Grid container spacing={1}>
          <Grid item md={1} xs={12}>
            <Typography variant='h6' className={classes.title}>Access Token:</Typography>
          </Grid>
          <Grid item md={11} xs={12}>
            <Typography className={classes.token}>{accessToken.accessToken}</Typography>
          </Grid>
        </Grid>
        <Button
          onClick={() => {
            navigator.clipboard.writeText(accessToken.accessToken)
          }}
          color="primary"
        >
          Copy to clipboard
        </Button>
      </Popover> : null }
  </>
}

export default AccessTokenPopup