import React from 'react';
import clsx from 'clsx';
import { makeStyles } from '@material-ui/styles';
import {List} from '@material-ui/core';
import {Page} from "../../Sidebar";
import PageNav from "./PageNav";

const useStyles = makeStyles((theme: any) => ({
  root: {
    padding: 2
  },
}));

interface SidebarNavProps {
  pages: Array<Page>,
  className: string
}

const SidebarNav: React.FC<SidebarNavProps> = props => {
  const { pages, className } = props

  const classes = useStyles()

  return (
    <List className={clsx(classes.root, className)} >
      {pages.map((page, index) => PageNav({page:page, index:index, layer:0}))}
    </List>
  );
}

export default SidebarNav;