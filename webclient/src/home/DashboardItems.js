import React from "react";

import List from "@material-ui/core/List";
import ListItem from "@material-ui/core/ListItem";
import {ListItemIcon, ListItemText} from "@material-ui/core";
import UpdateIcon from "@material-ui/icons/Update";
import BubbleChartIcon from "@material-ui/icons/BubbleChart";
import PeopleIcon from "@material-ui/icons/People";
import ListIcon from "@material-ui/icons/List";
import ErrorIcon from "@material-ui/icons/Error";

export function DashboardItems() {
  const [selectedPage, setSelectedPage] = React.useState("update");
  const handleListItemClick = (event, page) => {
    setSelectedPage(page);
  };

  return (
    <div>
      <ListItem
        button
        selected={selectedPage === "update"}
        onClick={(event) => handleListItemClick(event, "update")}>
        <ListItemIcon>
          <UpdateIcon/>
        </ListItemIcon>
        <ListItemText primary="Update" />
      </ListItem>
      <ListItem
        button
        selected={selectedPage === "services"}
        onClick={(event) => handleListItemClick(event, "services")}>
        <ListItemIcon>
          <BubbleChartIcon/>
        </ListItemIcon>
        <ListItemText primary="Services" />
      </ListItem>
      <ListItem
        button
        selected={selectedPage === "clients"}
        onClick={(event) => handleListItemClick(event, "clients")}>
        <ListItemIcon>
          <PeopleIcon/>
        </ListItemIcon>
        <ListItemText primary="Clients" />
      </ListItem>
      <ListItem
        button
        selected={selectedPage === "logging"}
        onClick={(event) => handleListItemClick(event, "logging")}>
        <ListItemIcon>
          <ListIcon/>
        </ListItemIcon>
        <ListItemText primary="Logging" />
      </ListItem>
      <ListItem
        button
        selected={selectedPage === "failures"}
        onClick={(event) => handleListItemClick(event, "failures")}>
        <ListItemIcon>
          <ErrorIcon/>
        </ListItemIcon>
        <ListItemText primary="Failures" />
      </ListItem>
    </div>
  )
}
