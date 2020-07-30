import React, {forwardRef} from "react";

import DashboardIcon from "@material-ui/icons/Dashboard";
import BubbleChartIcon from "@material-ui/icons/BubbleChart";
import PeopleIcon from "@material-ui/icons/People";
import ListIcon from "@material-ui/icons/List";
import ErrorIcon from "@material-ui/icons/Error";
import ExitToAppIcon from '@material-ui/icons/ExitToApp';
import { NavLink as RouterLink } from 'react-router-dom';
import ListItem from "@material-ui/core/ListItem";
import Button from "@material-ui/core/Button";
import { colors } from '@material-ui/core';
import makeStyles from "@material-ui/core/styles/makeStyles";
import List from "@material-ui/core/List";
import ListItemIcon from "@material-ui/core/ListItemIcon";
import ListItemText from "@material-ui/core/ListItemText";
import {light} from "@material-ui/core/styles/createPalette";

const pages = [
  {
    title: 'Dashboard',
    href: '/dashboard',
    icon: <DashboardIcon />
  },
  {
    title: 'Services',
    href: '/services',
    icon: <BubbleChartIcon />
  },
  {
    title: 'Clients',
    href: '/clients',
    icon: <PeopleIcon />
  },
  {
    title: 'Logging',
    href: '/logging',
    icon: <ListIcon />
  },
  {
    title: 'Failures',
    href: '/failures',
    icon: <ErrorIcon />
  },
  {
    title: 'Logout',
    href: '/login',
    icon: <ExitToAppIcon />
  }
]

const useStyles = makeStyles(theme => ({
  root: {},
  item: {
    display: 'flex',
    paddingLeft: 0,
    paddingRight: 0,
    paddingTop: 0,
    paddingBottom: 0
  },
  button: {
    color: colors.blueGrey[800],
    paddingLeft: 20, // TODO
    paddingRight: 20, // TODO
    justifyContent: 'flex-start',
    textTransform: 'none',
    letterSpacing: 0,
    width: '100%',
    fontWeight: theme.typography.fontWeightMedium
  },
  icon: {
    color: theme.palette.icon,
    width: 24,
    height: 24,
    display: 'flex',
    alignItems: 'center',
    marginRight: theme.spacing(1)
  },
  active: {
    background: colors.grey[300], // TODO
    fontWeight: theme.typography.fontWeightMedium,
    '& $icon': {
      color: theme.palette.primary.main
    }
  }
}));

const CustomRouterLink = forwardRef((props, ref) => (
  <div
    ref={ref}
    style={{ flexGrow: 1 }}
  >
    <RouterLink {...props} />
  </div>
));

function SidebarList() {
  const classes = useStyles();

  return (
    <List>
      {pages.map(page => (
        <ListItem
          className={classes.item}
          key={page.title}
        >
          <Button
            activeClassName={classes.active}
            className={classes.button}
            component={CustomRouterLink}
            to={page.href}
          >
            <ListItemIcon>{page.icon}</ListItemIcon>
            <ListItemText>{page.title}</ListItemText>
          </Button>
        </ListItem>
      ))}
    </List>
  )
}

export { SidebarList };
