import React from 'react';
import clsx from 'clsx';
import { makeStyles } from '@material-ui/styles';
import {
  Card,
  CardHeader,
  CardContent,
  Button,
  Divider, Box,
} from '@material-ui/core';
import AddIcon from '@material-ui/icons/Add';
import ConsumerAccountsTable from './ConsumerAccountsTable';
import {RouteComponentProps, useRouteMatch} from "react-router-dom";
import { NavLink as RouterLink } from 'react-router-dom';
import UserAccountsTable from "./UserAccountsTable";
import ServiceAccountsTable from "./ServiceAccountsTable";

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

interface AccountsManagerRouteParams {
  type: 'users' | 'services' | 'consumers'
}

const AccountManager: React.FC<RouteComponentProps<AccountsManagerRouteParams>> = props => {
  const accountsType = props.match.params.type

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
              Add New Account
            </Button>
          </Box>
        }
        title={accountsType == 'users'? 'Users'
             : accountsType == 'services' ? 'Services'
             : accountsType == 'consumers' ? 'Distribution Consumers'
             : ''}
      />
      <Divider/>
      <CardContent className={classes.content}>
        <div className={classes.inner}>
          { accountsType == 'users'? <UserAccountsTable/>
            : accountsType == 'services'? <ServiceAccountsTable/>
            : accountsType == 'consumers'? <ConsumerAccountsTable/>
            : null }
        </div>
      </CardContent>
    </Card>
  );
}

export default AccountManager