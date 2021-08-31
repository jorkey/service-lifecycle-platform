import React, { forwardRef } from 'react';
import {NavLink as RouterLink, useLocation} from 'react-router-dom';
import clsx from 'clsx';
import { makeStyles } from '@material-ui/styles';
import {List, ListItem, Button, colors, ListItemIcon, ListItemText, Collapse} from '@material-ui/core';
import {PageTitle, SinglePage} from "../../Sidebar";
import {ExpandLess, ExpandMore} from "@material-ui/icons";

const useStyles = makeStyles((theme: any) => ({
  root: {},
  item: {
    display: 'flex',
    paddingLeft: 0,
    paddingTop: 0,
    paddingBottom: 0
  },
  nestedItem: {
    paddingLeft: theme.spacing(10),
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
  },
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
  const [ openIndex, setOpenIndex ] = React.useState(new Map<number, boolean>());

  const classes = useStyles();

  const location = useLocation()

  const isOpened = (index: number, href: string) => {
    return openIndex.get(index) || openIndex.get(index) == undefined && location.pathname.startsWith(href)
  }

  return (
    <List className={clsx(classes.root, className)} >
      {pages.map((page, index) => (
        <div key={index}>
          <ListItem
            button
            className={classes.item}
            disableGutters
          >
            {(page.kind == 'single') ?
              (<Button
                activeClassName={classes.active}
                className={classes.button}
                component={CustomRouterLink}
                to={page.href}
              >
                <ListItemIcon className={classes.icon}>{page.icon}</ListItemIcon>
                {page.title}
              </Button>) :
              (<Button
                className={location.pathname.startsWith(page.href)?classes.active:classes.button}
                onClick={() => {
                  const opened = !isOpened(index, page.href)
                  setOpenIndex(new Map(openIndex.set(index, opened)))
                }}
              >
                <ListItemIcon className={classes.icon}>{page.icon}</ListItemIcon>
                {page.title}
                {isOpened(index, page.href) ? <ExpandLess/> : <ExpandMore/>}
              </Button>)
            }
          </ListItem>
          {(page.kind == 'title')?<Collapse in={isOpened(index, page.href)} timeout="auto" unmountOnExit>
            <List component="div">
              {page.pages.map((page, index) =>
                (<ListItem button className={classes.nestedItem} key={index}>
                  <Button
                    activeClassName={classes.active}
                    className={classes.button}
                    component={CustomRouterLink}
                    to={page.href}
                  >
                    {page.title}
                  </Button>
                </ListItem>))
              }
            </List>
          </Collapse>:null}
        </div>
      ))}
    </List>
  );
}

export default SidebarNav;