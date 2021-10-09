import React from 'react';
import clsx from 'clsx';
import PropTypes, {instanceOf} from 'prop-types';
import { makeStyles } from '@material-ui/styles';
import {Divider, Drawer} from '@material-ui/core';
import DashboardIcon from '@material-ui/icons/Dashboard';
import BubbleChartIcon from '@material-ui/icons/BubbleChart';
import AccountsIcon from '@material-ui/icons/AccountBox';
import PeopleIcon from '@material-ui/icons/People';
import ListIcon from '@material-ui/icons/List';
import ErrorIcon from '@material-ui/icons/Error';
import SettingsIcon from '@material-ui/icons/Settings';
import BuildIcon from '@material-ui/icons/Build';
import DistributionIcon from '@material-ui/icons/Share';
import ProfilesIcon from '@material-ui/icons/FilterList';

import { Profile, SidebarNav } from './components';
import {useDeveloperServicesQuery} from "../../../../generated/graphql";

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
  title: string
  href: string
  icon?: React.ReactElement
  pages?: Array<Page>
}

const Sidebar = (props:any) => {
  const { open, variant, onClose, className, ...rest } = props

  const classes = useStyles()

  const { data: developerServices } = useDeveloperServicesQuery()
  const development = !!developerServices?.developerServices?.length

  const buildPages: Page = development ? {
    title: 'Build',
    href: '/build/',
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
  } : {
    title: 'Build',
    href: '/build/client',
    icon: <BuildIcon />
  }

  const pages: Array<Page> = [
    {
      title: 'Dashboard',
      href: '/dashboard',
      icon: <DashboardIcon/>
    },
    buildPages,
    {
      title: 'Logging',
      href: '/logging',
      icon: <ListIcon/>
    },
    {
      title: 'Faults',
      href: '/faults',
      icon: <ErrorIcon/>
    },
    {
      title: 'Settings',
      href: '/settings/',
      icon: <SettingsIcon/>,
      pages: [
        {
          title: 'Accounts',
          href: '/settings/accounts/',
          icon: <AccountsIcon/>,
          pages: [
            {
              title: 'Users',
              href: '/settings/accounts/users',
              icon: <PeopleIcon/>,
            },
            {
              title: 'Services',
              href: '/settings/accounts/services',
              icon: <BubbleChartIcon/>,
            },
            {
              title: 'Distribution Consumers',
              href: '/settings/accounts/consumers',
              icon: <DistributionIcon/>,
            }
          ]
        },
        {
          title: 'Services',
          href: '/settings/services/',
          icon: <BubbleChartIcon/>,
          pages: [
            {
              title: 'Development',
              href: '/settings/services/development',
              icon: <BuildIcon/>,
            },
            {
              title: 'Profiles',
              href: '/settings/services/profiles',
              icon: <ProfilesIcon/>,
            }
          ]
        },
        {
          title: 'Providers',
          href: '/settings/providers',
          icon: <DistributionIcon/>,
        }
      ]
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
