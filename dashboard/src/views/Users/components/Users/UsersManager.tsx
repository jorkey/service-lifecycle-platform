import React from 'react';
import clsx from 'clsx';
import { makeStyles } from '@material-ui/styles';
import {
  Card,
  CardHeader,
  CardContent,
  Button,
  Divider,
} from '@material-ui/core';
import AddIcon from '@material-ui/icons/Add';
import FormGroup from '@material-ui/core/FormGroup';
import FormControlLabel from '@material-ui/core/FormControlLabel';
import {useUsersInfoQuery} from '../../../../generated/graphql';
import UsersTable from './UsersTable';
import EditIcon from "@material-ui/icons/Edit";

const useStyles = makeStyles(theme => ({
  root: {},
  content: {
    padding: 0
  },
  inner: {
    minWidth: 800
  },
  statusContainer: {
    display: 'flex',
    alignItems: 'center'
  },
  actions: {
    justifyContent: 'flex-end'
  },
  formControlLabel: {
    paddingLeft: '10px'
  }
}));

const UsersManager = () => {
  const classes = useStyles()
  const usersInfo = useUsersInfoQuery()

  return (
    <Card
      className={clsx(classes.root)}
    >
      <CardHeader
        action={
          <FormGroup row>
            <FormControlLabel
              className={classes.formControlLabel}
              label=''
              control={<Button
                onClick={() => {}}
              >
                <AddIcon
                  //onClick={(event) => setAnchor(event.currentTarget)}
                />
              </Button>
              }
            />
          </FormGroup>
        }
        title={'Users'}
      />
      <Divider />
      <CardContent className={classes.content}>
        <div className={classes.inner}>
          { usersInfo.data?(<UsersTable usersInfo={usersInfo.data.usersInfo}/>):null }
        </div>
      </CardContent>
    </Card>
  );
}

export default UsersManager