import React from 'react';
import { Link as RouterLink } from 'react-router-dom';
import { makeStyles } from '@material-ui/styles';
import {AppBar, Toolbar, Hidden, IconButton, Typography} from '@material-ui/core';
import MenuIcon from '@material-ui/icons/Menu';
import InputIcon from '@material-ui/icons/Input';
import BuildIcon from '@material-ui/icons/Build';
import Grid from '@material-ui/core/Grid';

const useStyles = makeStyles(theme => ({
  root: {
    boxShadow: 'none'
  },
  flexGrow: {
    flexGrow: 1
  },
  // signOutButton: {
  //   marginLeft: theme.spacing(1)
  // },
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
  onSidebarOpen: () => void,
}

const Topbar: React.FC<TopbarProps> = props => {
  const { /*className, */onSidebarOpen, ...rest } = props;

  const classes = useStyles();

  return (
    <AppBar
      {...rest}
      // className={clsx(classes.root, className)}
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
        <div className={classes.flexGrow} />
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
    </AppBar>
  );
};

export default Topbar;
