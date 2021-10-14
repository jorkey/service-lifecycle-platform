import React, {useEffect, useState} from 'react';

import Button from '@material-ui/core/Button';
import TextField from '@material-ui/core/TextField';
import Alert from '@material-ui/lab/Alert';

import { makeStyles } from '@material-ui/core/styles';
import {Grid} from '@material-ui/core';
import {useLoginMutation} from "../../generated/graphql";
import Cookies from "universal-cookie";
import {cookies} from "../../App";

const useStyles = makeStyles(theme => ({
  root: {
    padding: theme.spacing(4)
  },
  content: {
    paddingTop: 150,
    textAlign: 'center'
  },
  signIn: {
    marginTop: 20
  },
  alert: {
    marginTop: 20
  }
}));

interface LoginPageProps {
}

const LoginPage: React.FC<LoginPageProps> = () => {
  const classes = useStyles();

  useEffect(() => localStorage.removeItem('accessToken'), []);

  const [account, setAccount] = useState('');
  const [password, setPassword] = useState('');

  const [loginMutation, { data, loading, error }] =
    useLoginMutation({
      variables: { account: account, password: password },
      onError(err) {
        console.log(err);
      }})

  if (data) {
    cookies.set('accessToken', data.login, { path: '/' })
    localStorage.setItem('accessToken', data.login)
    console.log('token ' + localStorage.getItem('accessToken'))
    window.location.replace('/')
  }

  const handleAccountChange = (e: any) => {
    setAccount(e.target.value);
  }

  const handlePasswordChange = (e: any) => {
    setPassword(e.target.value);
  }

  const handleSubmit = (e: any) => {
    e.preventDefault();

    if (account && password) {
      loginMutation()
    }
  }

  return (
    <div className={classes.root}>
      <Grid
        container
        justifyContent='center'
        spacing={4}
      >
        <Grid
          item
          lg={2}
          md={3}
          xs={5}
        >
          <div className={classes.content}>
            <form name='form' onSubmit={handleSubmit}>
              <TextField
                variant='outlined'
                margin='normal'
                required
                fullWidth
                id='account'
                label='Account Name'
                name='account'
                autoComplete='email'
                autoFocus
                onChange={handleAccountChange}
              />
              <TextField
                variant='outlined'
                margin='normal'
                required
                fullWidth
                name='password'
                label='Password'
                type='password'
                id='password'
                autoComplete='current-password'
                onChange={handlePasswordChange}
              />
              <Button className={classes.signIn}
                disabled={loading}
                type='submit'
                fullWidth
                variant='contained'
                color='primary'
              >
                Sign In
              </Button>
              {error && <Alert className={classes.alert} severity='error'>{error.message}</Alert>}
            </form>
          </div>
        </Grid>
      </Grid>
    </div>
  )
}

export default LoginPage;
