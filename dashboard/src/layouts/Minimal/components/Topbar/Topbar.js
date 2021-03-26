import React, {useEffect, useState} from 'react';
import { Link as RouterLink } from 'react-router-dom';
import clsx from 'clsx';
import PropTypes from 'prop-types';
import { makeStyles } from '@material-ui/styles';
import {AppBar, Toolbar, Typography} from '@material-ui/core';
import Grid from '@material-ui/core/Grid';
import BuildIcon from '@material-ui/icons/Build';
import {Utils} from '../../../../common';
import {useDistributionInfoQuery} from "../../../../generated/graphql";

const useStyles = makeStyles(() => ({
  root: {
    boxShadow: 'none'
  },
  logo: {
    color: 'white',
    display: 'flex'
  },
  distributionName: {
    paddingLeft: 10,
    color: 'white',
    fontWeight: 400,
    fontSize: '24px',
    letterSpacing: '-0.06px',
    lineHeight: '28px'
  },
  version: {
    paddingLeft: 10,
    color: 'white',
    fontWeight: 100,
    fontSize: '24px',
    letterSpacing: '-0.06px',
    lineHeight: '28px'
  }
}));

const Topbar = props => {
  const { className, ...rest } = props;

  const classes = useStyles();

  const { loading, error, data } = useDistributionInfoQuery();

  return (
    <AppBar
      {...rest}
      className={clsx(classes.root, className)}
      color='primary'
      position='fixed'
    >
      <Toolbar>
        <RouterLink to='/'>
          <Grid className={classes.logo}>
            <BuildIcon/>
            { data ? (
              <>
                <Typography className={classes.distributionName}
                            display='inline'
                >
                  {data.distributionInfo.title}
                </Typography>
              </>
            ) : null }
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
