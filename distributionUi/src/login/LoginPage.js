import React from 'react';
import { Utils } from '../utils';

import Button from '@material-ui/core/Button';
import CssBaseline from '@material-ui/core/CssBaseline';
import TextField from '@material-ui/core/TextField';
import Alert from '@material-ui/lab/Alert';

import { makeStyles } from '@material-ui/core/styles';
import Container from '@material-ui/core/Container';

let classes = makeStyles(theme => ({
  paper: {
    marginTop: theme.spacing(8),
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
  },
  avatar: {
    margin: theme.spacing(1),
    backgroundColor: theme.palette.secondary.main,
  },
  form: {
    width: '100%', // Fix IE 11 issue.
    marginTop: theme.spacing(1),
  },
  submit: {
    margin: theme.spacing(3, 0, 2),
  }
}));

class LoginPage extends React.Component {
  constructor(props) {
    super(props);

    Utils.logout();

    this.state = {
      username: '',
      password: '',
      error: ''
    };

    this.handleChange = this.handleChange.bind(this);
    this.handleSubmit = this.handleSubmit.bind(this);
  }

  handleChange(e) {
    const { name, value } = e.target;
    this.setState({ [name]: value });
  }

  handleSubmit(e) {
    e.preventDefault();

    this.setState({ submitted: true });
    const { username, password } = this.state;

    if (!(username && password)) {
      return;
    }

    this.setState({ loading: true });
    Utils.login(username, password).then(
      user => {
        const { from } = this.props.location.state || { from: { pathname: "/" } };
        this.props.history.push(from);
      },
      response => {
        const error = (response.status === 401)?"Authorization error":response.status + ": " + response.statusText;
        console.log("Login error " + error)
        this.setState({ error: error.toString(), loading: false })
      }
    );
  }

  render() {
    const { username, password, submitted, loading, error } = this.state;
    return (
      <Container component="main" maxWidth="xs">
        <CssBaseline />
        <div className={classes.paper}>
          <h2>Update Distribution Server</h2>
          <form name="form" onSubmit={this.handleSubmit}>
            <TextField
              variant="outlined"
              margin="normal"
              required
              fullWidth
              id="username"
              label="User Name"
              name="username"
              autoComplete="email"
              autoFocus
              onChange={this.handleChange}
            />
            <TextField
              variant="outlined"
              margin="normal"
              required
              fullWidth
              name="password"
              label="Password"
              type="password"
              id="password"
              autoComplete="current-password"
              onChange={this.handleChange}
            />
            <Button
              disabled={loading}
              type="submit"
              fullWidth
              variant="contained"
              color="primary"
              className={classes.submit}
            >
              Sign In
            </Button>
            {error && <Alert severity="error">{error}</Alert>}
          </form>
        </div>
      </Container>
    );
  }
}

export { LoginPage };
