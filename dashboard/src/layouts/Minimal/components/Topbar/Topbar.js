import React, {useEffect, useState} from 'react';
import { Link as RouterLink } from 'react-router-dom';
import clsx from 'clsx';
import PropTypes from 'prop-types';
import { makeStyles } from '@material-ui/styles';
import {AppBar, Toolbar, Typography} from '@material-ui/core';
import Grid from '@material-ui/core/Grid';
import BuildIcon from '@material-ui/icons/Build';
import {Utils} from '../../../../common';

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

  const [distributionInfo, setDistributionInfo] = useState([]);

  useEffect(() => {
    Utils.getDistributionInfo().then(info => setDistributionInfo(info))
  }, [])

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
            { distributionInfo.name ? (
              <>
                <Typography className={classes.distributionName}
                            display='inline'
                >
                  {distributionInfo.name}
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
