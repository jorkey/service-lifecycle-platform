import React, {useState} from 'react';
import clsx from 'clsx';
import { makeStyles } from '@material-ui/styles';
import {
  Card,
  CardHeader,
  CardContent,
  Button,
  Divider, IconButton, Select, Box,
} from '@material-ui/core';
import AddIcon from '@material-ui/icons/Add';
import FormGroup from '@material-ui/core/FormGroup';
import FormControlLabel from '@material-ui/core/FormControlLabel';
import UsersTable from './UsersTable';
import {type} from "os";

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
  const [usersType, setUsersType] = useState('people')

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
                value={usersType}
                onChange={(event) =>
                  setUsersType(event.target.value as string)}
              >
                <option value='people'>People</option>
                <option value='services'>Services</option>
            </Select>
            <Button
              color="primary"
              variant="contained"
              className={classes.formControl}
              startIcon={<AddIcon/>}
              onClick={() => {}}
            >
              Add New
            </Button>
          </Box>
        }
        title={'Users'}
      />
      <Divider/>
      <CardContent className={classes.content}>
        <div className={classes.inner}>
          <UsersTable people={usersType=='people'}/>
        </div>
      </CardContent>
    </Card>
  );
}

export default UsersManager