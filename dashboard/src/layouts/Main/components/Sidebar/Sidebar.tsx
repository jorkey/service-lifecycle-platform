import React from 'react';
import clsx from 'clsx';
import PropTypes from 'prop-types';
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
import DesiredVersionsIcon from '@material-ui/icons/Map';
import DistributionIcon from '@material-ui/icons/Share';
import ProfilesIcon from '@material-ui/icons/FilterList';
import TaskIcon from '@material-ui/icons/Check';

import {SidebarNav} from './components';
import {useBuildDeveloperServicesQuery} from "../../../../generated/graphql";
import DistributionVersion from "./components/DistributionVersion/DistributionVersion";

const useStyles = makeStyles((theme?: any) => ({
  drawer: {
    width: 260,
    [theme.breakpoints.up('lg')]: {
      marginTop: 48,
      height: 'calc(100% - 48px)'
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
    margin: theme.spacing(1, 0)
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

  const { data: developerServices } = useBuildDeveloperServicesQuery({
    fetchPolicy: 'no-cache', // base option no-cache does not work
  })

  if (developerServices?.buildDeveloperServicesConfig) {
    const development = !!developerServices?.buildDeveloperServicesConfig.length

    const buildPages: Page = development ? {
      title: 'Build',
      href: '/build/',
      icon: <BuildIcon/>,
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
      icon: <BuildIcon/>
    }

    const desiredVersionsPages: Page = development ? {
      title: 'Desired Versions',
      href: '/desiredVersions/',
      icon: <DesiredVersionsIcon/>,
      pages: [
        {
          title: 'Developer',
          href: '/desiredVersions/developer',
        },
        {
          title: 'Client',
          href: '/desiredVersions/client',
        }
      ]
    } : {
      title: 'Desired Versions',
      href: '/desiredVersions/client',
      icon: <DesiredVersionsIcon/>
    }

    const pages: Array<Page> = [
      {
        title: 'Dashboard',
        href: '/dashboard',
        icon: <DashboardIcon/>
      },
      buildPages,
      desiredVersionsPages,
      {
        title: 'Logging',
        href: '/logging',
        icon: <ListIcon/>,
        pages: [
          {
            title: 'Tasks',
            href: '/logging/tasks',
            icon: <TaskIcon/>,
          },
          {
            title: 'Services',
            href: '/logging/services',
            icon: <BubbleChartIcon/>,
          },
        ]
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
            title: 'Build',
            href: '/settings/build/',
            icon: <BuildIcon/>,
            pages: [
              {
                title: 'Developer',
                href: '/settings/build/developer'
              },
              {
                title: 'Client',
                href: '/settings/build/client'
              }
            ]
          },
          {
            title: 'Profiles',
            href: '/settings/profiles',
            icon: <ProfilesIcon/>,
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
        classes={{paper: classes.drawer}}
        onClose={onClose}
        open={open}
        variant={variant}
      >
        <div
          {...rest}
          className={clsx(classes.root, className)}
        >
          <DistributionVersion/>
          <Divider className={classes.divider}/>
          <SidebarNav
            className={classes.nav}
            pages={pages}
          />
        </div>
      </Drawer>
    )
  } else {
    return null
  }
}

Sidebar.propTypes = {
  className: PropTypes.string,
  onClose: PropTypes.func,
  open: PropTypes.bool.isRequired,
  variant: PropTypes.string.isRequired
};

export default Sidebar;
