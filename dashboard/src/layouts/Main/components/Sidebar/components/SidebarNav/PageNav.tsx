import React, { forwardRef } from 'react';
import {NavLink as RouterLink, useLocation} from 'react-router-dom';
import { makeStyles } from '@material-ui/styles';
import {List, ListItem, Button, colors, ListItemIcon, Collapse, Theme} from '@material-ui/core';
import {ExpandLess, ExpandMore} from "@material-ui/icons";
import {Page} from "../../Sidebar";

const useStyles = makeStyles<Theme, PageNavProps>((theme: any) => ({
  root: {},
  item: {
    display: 'flex',
    paddingLeft: props => theme.spacing(2 * props.layer),
    paddingTop: 0,
    paddingBottom: 0
  },
  page: {
    color: colors.blueGrey[800],
    justifyContent: 'flex-start',
    letterSpacing: 0,
    width: '100%',
    fontWeight: theme.typography.fontWeightMedium,
    display: 'flex',
    marginRight: theme.spacing(1)
  },
  active: {
    color: theme.palette.primary.main,
    '& $icon': {
      color: theme.palette.primary.main
    }
  },
  icon: {
    color: theme.palette.icon,
    width: 24,
    height: 24,
    display: 'flex',
    alignItems: 'center',
    marginRight: theme.spacing(1)
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

interface PageNavProps {
  page: Page
  index: number
  layer: number
}

const PageNav: React.FC<PageNavProps> = props => {
  const { page, index, layer } = props

  const [ opened, setOpened ] = React.useState<boolean>()

  const classes = useStyles(props)

  const location = useLocation()

  const isOpened = () => opened || (isSelected() && opened == undefined)
  const isSelected = () => location.pathname.startsWith(page.href)

  return (<div key={index}>
    <ListItem
      button
      className={classes.item}
      disableGutters
    >
      { page.pages ?
        (<Button
          className={isSelected()?`${classes.page} ${classes.active}`:classes.page}
          onClick={() => setOpened(!isOpened())}
        >
          {page.icon?<ListItemIcon className={classes.icon}>{page.icon}</ListItemIcon>:null}
          {page.title}
          {isOpened() ? <ExpandLess/> : <ExpandMore/>}
        </Button>) :
        (<Button
          activeClassName={`${classes.page} ${classes.active}`}
          className={classes.page}
          component={CustomRouterLink}
          to={page.href}
        >
          <ListItemIcon className={classes.icon}>{page.icon}</ListItemIcon>
          {page.title}
        </Button>)
      }
    </ListItem>
    {page.pages?<Collapse in={isOpened()} timeout="auto" unmountOnExit>
      <List component="div">
        {page.pages.map((page, index) => PageNav({page:page, index:index, layer: layer+1}))}
      </List>
    </Collapse>:null}
  </div>)
}

export default PageNav