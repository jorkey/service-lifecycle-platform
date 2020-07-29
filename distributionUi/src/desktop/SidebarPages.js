import React, {forwardRef} from "react";

import DashboardIcon from "@material-ui/icons/Dashboard";
import BubbleChartIcon from "@material-ui/icons/BubbleChart";
import PeopleIcon from "@material-ui/icons/People";
import ListIcon from "@material-ui/icons/List";
import ErrorIcon from "@material-ui/icons/Error";
import { NavLink as RouterLink } from 'react-router-dom';
import ListItem from "@material-ui/core/ListItem";
import Button from "@material-ui/core/Button";
import { colors } from '@material-ui/core';
import makeStyles from "@material-ui/core/styles/makeStyles";
import List from "@material-ui/core/List";

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
  }
]

const useStyles = makeStyles(theme => ({
  root: {},
  item: {
    display: 'flex',
    paddingTop: 0,
    paddingBottom: 0
  },
  button: {
    color: colors.blueGrey[800],
    padding: '10px 8px',
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
    color: theme.palette.primary.main,
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

function SidebarPagesList() {
  const classes = useStyles();

  return (
    <List
   //   {...rest}
   //   className={clsx(classes.root, className)}
    >
      {pages.map(page => (
        <ListItem
          className={classes.item}
          disableGutters
          key={page.title}
        >
          <Button
            activeClassName={classes.active}
            className={classes.button}
            component={CustomRouterLink}
            to={page.href}
          >
            <div className={classes.icon}>{page.icon}</div>
            {page.title}
          </Button>
        </ListItem>
      ))}
    </List>
  )
}

export { SidebarPagesList };
