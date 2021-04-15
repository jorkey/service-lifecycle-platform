import React, {useState} from 'react';
import clsx from 'clsx';
import { makeStyles } from '@material-ui/styles';
import {
  Card,
  CardHeader,
  CardContent,
  Button,
  Divider, IconButton, Select, Box, Link,
} from '@material-ui/core';
import AddIcon from '@material-ui/icons/Add';
import UsersTable from './UsersTable';
import {useRouteMatch} from "react-router-dom";

const useStyles = makeStyles(theme => ({
  root: {},
  content: {
    padding: 0
  },
  inner: {
    minWidth: 800
  },
  actions: {
    display: 'flex',
    justifyContent: 'flex-end'
  },
  formControl: {
    marginLeft: '50px'
  }
}));

const UsersManager = () => {
  const classes = useStyles()
  const [people, setPeople] = useState(true)
  const routeMatch = useRouteMatch();

  return (
    <Card
      className={clsx(classes.root)}
    >
      <CardHeader
        action={
          <Box
            className={classes.actions}
          >
            <Select
                className={classes.formControl}
                native
                title='Type'
                value={people?'people':'services'}
                onChange={(event) =>
                  setPeople(event.target.value === 'people')}
              >
                <option value='people'>People</option>
                <option value='services'>Services</option>
            </Select>
            <Link href={`${routeMatch.url}/new/${people?'human':'service'}`}>
              <Button
                color="primary"
                variant="contained"
                className={classes.formControl}
                startIcon={<AddIcon/>}
                onClick={() => {}}
              >
                Add New
              </Button>
            </Link>
          </Box>
        }
        title={'Users'}
      />
      <Divider/>
      <CardContent className={classes.content}>
        <div className={classes.inner}>
          <UsersTable people={people}/>
        </div>
      </CardContent>
    </Card>
  );
}

export default UsersManager