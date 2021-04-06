import React from 'react';
import { Link as RouterLink } from 'react-router-dom';
import clsx from 'clsx';
import PropTypes from 'prop-types';
import { makeStyles } from '@material-ui/styles';
import {AppBar, Toolbar, Typography} from '@material-ui/core';
import Grid from '@material-ui/core/Grid';
import BuildIcon from '@material-ui/icons/Build';
import {Utils} from '../../../../common';
import {DistributionInfo, useDistributionInfoQuery} from "../../../../generated/graphql";

const useStyles = makeStyles(() => ({
  root: {
    boxShadow: 'none'
  },
  logo: {
    color: 'white',
    display: 'flex'
  },
  distributionTitle: {
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

interface TopbarProps {
  // className: string,
}

const Topbar: React.FC<TopbarProps> = props => {
  const classes = useStyles();

  return (
    <AppBar
      {...props}
      // className={clsx(classes.root, className)}
      color='primary'
      position='fixed'
    >
      <Toolbar>
        <RouterLink to='/'>
          <Grid className={classes.logo}>
            <BuildIcon/>
              <Typography className={classes.distributionTitle}
                          display='inline'
              >
                {localStorage.getItem('distributionTitle')}
              </Typography>
          </Grid>
        </RouterLink>
      </Toolbar>
    </AppBar>
  );
};

export default Topbar;
