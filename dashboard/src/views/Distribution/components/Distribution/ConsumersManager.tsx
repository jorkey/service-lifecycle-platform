import React, {useState} from 'react';
import clsx from 'clsx';
import { makeStyles } from '@material-ui/styles';
import {
  Card,
  CardHeader,
  CardContent,
  Button,
  Divider, Box, Link,
} from '@material-ui/core';
import AddIcon from '@material-ui/icons/Add';
import {RouteComponentProps, useRouteMatch} from "react-router-dom";
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
    textTransform: 'none'
  }
}));

interface ConsumersManagerParams {
}

const ConsumersManager: React.FC<ConsumersManagerParams> = props => {
  const classes = useStyles()
  const routeMatch = useRouteMatch();

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
              variant="contained"
              className={classes.control}
              startIcon={<AddIcon/>}
              component={RouterLink}
              to={`${routeMatch.url}/new`}
            >
              Add New Consumer
            </Button>
          </Box>
        }
        title={'Consumers'}
      />
      <Divider/>
      <CardContent className={classes.content}>
        <div className={classes.inner}>
          {/*<UsersTable userType={userType}/>*/}
        </div>
      </CardContent>
    </Card>
  );
}

export default ConsumersManager