import React, {useEffect, useState} from 'react';

import Button from '@material-ui/core/Button';
import TextField from '@material-ui/core/TextField';
import Alert from '@material-ui/lab/Alert';

import { makeStyles } from '@material-ui/core/styles';
import {Grid} from '@material-ui/core';
import {useLoginMutation} from "../../generated/graphql";

const useStyles = makeStyles(theme => ({
  root: {
    padding: theme.spacing(4)
  },
  content: {
    paddingTop: 150,
    textAlign: 'center'
  }
}));

interface LoginPageProps {
}

const LoginPage: React.FC<LoginPageProps> = () => {
  const classes = useStyles();

  useEffect(() => localStorage.removeItem('token'), []);

  const [user, setUser] = useState('');
  const [password, setPassword] = useState('');

  const [loginMutation, { data, loading, error }] =
    useLoginMutation({
      variables: { user: user, password: password },
      onError(err) {
        console.log(err);
      }})

  if (data) {
    console.log('token ' + data.login)
    localStorage.setItem('token', data.login)
    window.location.replace('/')
  }

  const handleUserChange = (e: any) => {
    setUser(e.target.value);
  }

  const handlePasswordChange = (e: any) => {
    setPassword(e.target.value);
  }

  const handleSubmit = (e: any) => {
    e.preventDefault();

    if (user && password) {
      loginMutation()
    }
  }

  return (
    <div className={classes.root}>
      <Grid
        container
        justify='center'
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
                id='user'
                label='User Name'
                name='user'
                autoComplete='email'
                autoFocus
                onChange={handleUserChange}
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
              <Button
                disabled={loading}
                type='submit'
                fullWidth
                variant='contained'
                color='primary'
              >
                Sign In
              </Button>
              {error && <Alert severity='error'>{error.message}</Alert>}
            </form>
          </div>
        </Grid>
      </Grid>
    </div>
  )
}

export default LoginPage;
