import React, {useState} from 'react';
import clsx from 'clsx';
import { makeStyles } from '@material-ui/styles';
import {
  Card,
  CardHeader,
  CardContent,
  Button,
  Box, Link,
} from '@material-ui/core';
import AddIcon from '@material-ui/icons/Add';
import ProfilesTable from './ProfilesTable';
import {useRouteMatch} from "react-router-dom";
import { NavLink as RouterLink } from 'react-router-dom';

const useStyles = makeStyles((theme:any) => ({
  root: {},
  content: {
    padding: 0
  },
  inner: {
    minWidth: 800
  },
  controls: {
    display: 'flex',
    justifyContent: 'flex-end'
  },
  control: {
    marginLeft: '50px',
  }
}));

const ProfilesManager = () => {
  const classes = useStyles()
  const routeMatch = useRouteMatch()

  return (
    <Card
      className={clsx(classes.root)}
    >
      <CardHeader
        action={
          <Box
            className={classes.controls}
          >
            <Button
              color="primary"
              className={classes.control}
              startIcon={<AddIcon/>}
              title={'Add profile'}
              component={RouterLink}
              to={`${routeMatch.url}/new`}
            />
          </Box>
        }
        title={'Service Profiles'}
      />
      <CardContent className={classes.content}>
        <div className={classes.inner}>
          <ProfilesTable/>
        </div>
      </CardContent>
    </Card>
  );
}

export default ProfilesManager