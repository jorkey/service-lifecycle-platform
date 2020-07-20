import React from 'react';
import ListItem from '@material-ui/core/ListItem';
import { ListItemIcon, ListItemText } from '@material-ui/core';
import { Update, BubbleChart, People, List, Error } from '@material-ui/icons';

export const mainListItems = (
  <div>
    <ListItem button>
      <ListItemIcon>
        <Update />
      </ListItemIcon>
      <ListItemText primary="Update" />
    </ListItem>
    <ListItem button>
      <ListItemIcon>
        <BubbleChart />
      </ListItemIcon>
      <ListItemText primary="Services" />
    </ListItem>
    <ListItem button>
      <ListItemIcon>
        <People />
      </ListItemIcon>
      <ListItemText primary="Clients" />
    </ListItem>
    <ListItem button>
      <ListItemIcon>
        <List />
      </ListItemIcon>
      <ListItemText primary="Logging" />
    </ListItem>
    <ListItem button>
      <ListItemIcon>
        <Error />
      </ListItemIcon>
      <ListItemText primary="Failures" />
    </ListItem>
  </div>
);