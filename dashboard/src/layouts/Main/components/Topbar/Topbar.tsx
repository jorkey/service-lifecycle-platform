import React from 'react';
import { Link as RouterLink } from 'react-router-dom';
import { makeStyles } from '@material-ui/styles';
import {AppBar, Toolbar, Hidden, IconButton, Typography} from '@material-ui/core';
import MenuIcon from '@material-ui/icons/Menu';
import InputIcon from '@material-ui/icons/Input';
import BuildIcon from '@material-ui/icons/Build';
import Grid from '@material-ui/core/Grid';
import {useDistributionInfoQuery, useWhoAmIQuery} from "../../../../generated/graphql";

const useStyles = makeStyles(theme => ({
  root: {
    boxShadow: 'none'
  },
  toolbar: {
    minHeight: '48px'
  },
  logo: {
    color: 'white',
    display: 'flex'
  },
  icon: {
    paddingTop: '4px'
  },
  distributionTitle: {
    paddingLeft: 10,
    color: 'white',
    fontWeight: 400,
    fontSize: '20px',
    letterSpacing: '-0.06px',
    lineHeight: '28px'
  },
  distributionVersion: {
    paddingLeft: 10,
    color: 'white',
    fontWeight: 400,
    fontSize: '20px',
    letterSpacing: '-0.06px',
    lineHeight: '28px'
  },
  flexGrow: {
    flexGrow: 1
  },
  // signOutButton: {
  //   marginLeft: theme.spacing(1)
  // },
  accountName: {
    paddingLeft: 10,
    color: 'white',
    fontWeight: 400,
    fontSize: '20px',
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
  onSidebarOpen: () => void,
}

const Topbar: React.FC<TopbarProps> = props => {
  const { /*className, */onSidebarOpen, ...rest } = props;

  const classes = useStyles();

  const { data:whoAmI } = useWhoAmIQuery()

  return (whoAmI?.whoAmI ?
    <AppBar
      {...rest}
      // className={clsx(classes.root, className)}
    >
      <Toolbar className={classes.toolbar}>
        <RouterLink to='/'>
          <Grid className={classes.logo}>
            <BuildIcon className={classes.icon}/>
            <Typography className={classes.distributionTitle}
              display='inline'
            >
              {localStorage.getItem('distributionTitle')}
            </Typography>
          </Grid>
        </RouterLink>
        <div className={classes.flexGrow} />
        <Typography className={classes.accountName}
                    display='inline'
        >
          {whoAmI.whoAmI.name}
        </Typography>
        <Hidden mdDown>
          <IconButton title='Logout'
            // className={classes.signOutButton}
            color='inherit'
            href='/login'
          >
          <InputIcon />
          </IconButton>
        </Hidden>
        <Hidden lgUp>
          <IconButton
            color='inherit'
            onClick={onSidebarOpen}
          >
            <MenuIcon />
          </IconButton>
        </Hidden>
      </Toolbar>
    </AppBar> : null
  );
};

export default Topbar;
