import React from "react";

import ListItem from "@material-ui/core/ListItem";
import {ListItemIcon, ListItemText} from "@material-ui/core";
import UpdateIcon from "@material-ui/icons/Update";
import BubbleChartIcon from "@material-ui/icons/BubbleChart";
import PeopleIcon from "@material-ui/icons/People";
import ListIcon from "@material-ui/icons/List";
import ErrorIcon from "@material-ui/icons/Error";
import NavLink from "react-bootstrap/NavLink";
import Clients from "../views/Clients";

function SidebarItems() {
  return (
    <div>
      <ListItem button>
        <ListItemIcon>
          <UpdateIcon/>
        </ListItemIcon>
        <ListItemText primary="Update" />
      </ListItem>
      <ListItem button>
        <NavLink to='/services' href='/services'>
          <ListItemIcon>
            <BubbleChartIcon/>
          </ListItemIcon>
          <ListItemText primary="Services" />
        </NavLink>
      </ListItem>
      <ListItem button>
        <NavLink to='/clients' href='/clients'>
          <ListItemIcon>
            <PeopleIcon/>
          </ListItemIcon>
          <ListItemText primary="Clients" />
        </NavLink>
      </ListItem>
      <ListItem
        button>
        <ListItemIcon>
          <ListIcon/>
        </ListItemIcon>
        <ListItemText primary="Logging" />
      </ListItem>
      <ListItem
        button>
        <ListItemIcon>
          <ErrorIcon/>
        </ListItemIcon>
        <ListItemText primary="Failures" />
      </ListItem>
    </div>
  )
}

export { SidebarItems };
