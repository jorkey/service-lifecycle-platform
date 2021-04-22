import React, { forwardRef } from 'react';
import { NavLink as RouterLink } from 'react-router-dom';
import clsx from 'clsx';
import { makeStyles } from '@material-ui/styles';
import { List, ListItem, Button, colors } from '@material-ui/core';
import {PageTitle, SinglePage} from "../../Sidebar";
import {instanceOf} from "prop-types";

const useStyles = makeStyles((theme: any) => ({
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

const CustomRouterLink = forwardRef((props: any, ref: any) => (
  <div
    ref={ref}
    style={{ flexGrow: 1 }}
  >
    <RouterLink {...props} />
  </div>
));

interface SidebarNavProps {
  pages: Array<SinglePage|PageTitle>,
  className: string
}

const SidebarNav: React.FC<SidebarNavProps> = props => {
  const { pages, className } = props;

  const classes = useStyles();

  // TODO implement expand lists
  // <ListItem button onClick={handleClick}>
  //   <ListItemIcon>
  //     <InboxIcon />
  //   </ListItemIcon>
  //   <ListItemText primary="Inbox" />
  //   {open ? <ExpandLess /> : <ExpandMore />}
  // </ListItem>
  // <Collapse in={open} timeout="auto" unmountOnExit>
  //   <List component="div" disablePadding>
  //     <ListItem button className={classes.nested}>
  //       <ListItemIcon>
  //         <StarBorder />
  //       </ListItemIcon>
  //       <ListItemText primary="Starred" />
  //     </ListItem>
  //   </List>
  // </Collapse>

  return (
    <List
      className={clsx(classes.root, className)}
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
  );
};

export default SidebarNav;
