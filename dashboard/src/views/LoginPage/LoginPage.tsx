import React, {useEffect, useState} from 'react';
import { Utils } from '../../common';

import Button from '@material-ui/core/Button';
import TextField from '@material-ui/core/TextField';
import Alert from '@material-ui/lab/Alert';

import { makeStyles } from '@material-ui/core/styles';
import {Grid} from '@material-ui/core';
import {useMutation} from "@apollo/client";

const useStyles = makeStyles(theme => ({
  root: {
    padding: theme.spacing(4)
  },
  content: {
    paddingTop: 150,
    textAlign: 'center'
  }
}));

const LoginPage = () => {
  const classes = useStyles();

  useEffect(() => Utils.logout(), []);

  const [userName, setUserName] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleUserNameChange = (e: any) => {
    setUserName(e.target.value);
  }

  const handlePasswordChange = (e: any) => {
    setPassword(e.target.value);
  }

  const handleSubmit = (e: any) => {
    e.preventDefault();

    if (!(userName && password)) {
      return;
    }

    setLoading(true);

    Utils.login(userName, password).then(
      user => {
        window.location.replace('/')
      },
      response => {
        const error = (response.status === 401)?'Authorization error':response.status + ': ' + response.statusText;
        console.log('Login error ' + error)
        setLoading(false)
        setError(error.toString())
      }
    );
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
                id='username'
                label='User Name'
                name='username'
                autoComplete='email'
                autoFocus
                onChange={handleUserNameChange}
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
              {error && <Alert severity='error'>{error}</Alert>}
            </form>
          </div>
        </Grid>
      </Grid>
    </div>
  )
}

export default LoginPage;
