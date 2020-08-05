import React from 'react';
import { Link as RouterLink } from 'react-router-dom';
import clsx from 'clsx';
import PropTypes from 'prop-types';
import { makeStyles } from '@material-ui/styles';
import {AppBar, Toolbar, Typography} from '@material-ui/core';
import Grid from "@material-ui/core/Grid";
import UpdateIcon from "@material-ui/icons/Update";

const useStyles = makeStyles(() => ({
  root: {
    boxShadow: 'none'
  },
  logo: {
    color: 'white',
    display: 'flex'
  },
  logotype: {
    paddingLeft: 10,
    color: 'white',
    fontWeight: 400,
    fontSize: '24px',
    letterSpacing: '-0.06px',
    lineHeight: '28px'
  }
}));

const Topbar = props => {
  const { className, ...rest } = props;

  const classes = useStyles();

  return (
    <AppBar
      {...rest}
      className={clsx(classes.root, className)}
      color="primary"
      position="fixed"
    >
      <Toolbar>
        <RouterLink to="/">
          <Grid className={classes.logo}>
            <UpdateIcon/>
            <Typography className={classes.logotype}
                        display="inline"
            >
              Update Dashboard
            </Typography>
          </Grid>
        </RouterLink>
      </Toolbar>
    </AppBar>
  );
};

Topbar.propTypes = {
  className: PropTypes.string
};

export default Topbar;
