import React from 'react';
import clsx from 'clsx';
import PropTypes, {instanceOf} from 'prop-types';
import { makeStyles } from '@material-ui/styles';
import {Divider, Drawer} from '@material-ui/core';
import DashboardIcon from '@material-ui/icons/Dashboard';
import BubbleChartIcon from '@material-ui/icons/BubbleChart';
import PeopleIcon from '@material-ui/icons/People';
import ListIcon from '@material-ui/icons/List';
import ErrorIcon from '@material-ui/icons/Error';
import ShareIcon from '@material-ui/icons/Share';
import BuildIcon from '@material-ui/icons/Build';

import { Profile, SidebarNav } from './components';

const useStyles = makeStyles((theme?: any) => ({
  drawer: {
    width: 240,
    [theme.breakpoints.up('lg')]: {
      marginTop: 64,
      height: 'calc(100% - 64px)'
    }
  },
  root: {
    backgroundColor: theme.palette.white,
    display: 'flex',
    flexDirection: 'column',
    height: '100%',
    padding: theme.spacing(2)
  },
  divider: {
    margin: theme.spacing(2, 0)
  },
  nav: {
    marginBottom: theme.spacing(2)
  }
}));

export interface Page {
  title: string,
  href: string
}

export interface BarItem {
  title: string,
  icon: React.ReactElement
}

export interface SinglePage extends BarItem, Page {
  kind: 'single'
}

export interface PageTitle extends BarItem {
  kind: 'title'
  pages: Array<Page>
}

const Sidebar = (props:any) => {
  const { open, variant, onClose, className, ...rest } = props;

  const classes = useStyles();

  const pages: Array<SinglePage|PageTitle> = [
    {
      kind: 'single',
      title: 'Dashboard',
      href: '/dashboard',
      icon: <DashboardIcon />
    },
    {
      kind: 'title',
      title: 'Users',
      icon: <PeopleIcon />,
      pages: [
        {
          title: 'People',
          href: '/users/human',
        },
        {
          title: 'Services',
          href: '/users/service',
        }
      ]
    },
    {
      kind: 'title',
      title: 'Services',
      icon: <BubbleChartIcon />,
      pages: [
        {
          title: 'Development',
          href: '/services/development',
        },
        {
          title: 'Profiles',
          href: '/services/profiles',
        }
      ]
    },
    {
      kind: 'title',
      title: 'Distribution',
      icon: <ShareIcon />,
      pages: [
        {
          title: 'Provider',
          href: '/distribution/provider',
        },
        {
          title: 'Consumers',
          href: '/distribution/consumers',
        }
      ]
    },
    {
      kind: 'title',
      title: 'Build',
      icon: <BuildIcon />,
      pages: [
        {
          title: 'Developer',
          href: '/build/developer',
        },
        {
          title: 'Client',
          href: '/build/client',
        }
      ]
    },
    {
      kind: 'single',
      title: 'Logging',
      href: '/logging',
      icon: <ListIcon />
    },
    {
      kind: 'single',
      title: 'Failures',
      href: '/failures',
      icon: <ErrorIcon />
    }
  ];

  return (
    <Drawer
      anchor='left'
      classes={{ paper: classes.drawer }}
      onClose={onClose}
      open={open}
      variant={variant}
    >
      <div
        {...rest}
        className={clsx(classes.root, className)}
      >
        <Profile />
        <Divider className={classes.divider} />
        <SidebarNav
          className={classes.nav}
          pages={pages}
        />
      </div>
    </Drawer>
  );
};

Sidebar.propTypes = {
  className: PropTypes.string,
  onClose: PropTypes.func,
  open: PropTypes.bool.isRequired,
  variant: PropTypes.string.isRequired
};

export default Sidebar;
