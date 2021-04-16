import React, {useState} from 'react';

import Button from '@material-ui/core/Button';
import TextField from '@material-ui/core/TextField';
import { RouteComponentProps } from "react-router-dom"

import { makeStyles } from '@material-ui/core/styles';
import {Box, Card, CardContent, CardHeader, Checkbox, Divider, FormControlLabel, FormGroup} from '@material-ui/core';
import {
  useAddUserMutation,
  useChangeUserMutation,
  UserRole,
  useUserInfoLazyQuery, useWhoAmIQuery
} from '../../../../generated/graphql';
import clsx from 'clsx';
import Alert from "@material-ui/lab/Alert";
import {GraphQLError} from "graphql";

const useStyles = makeStyles(theme => ({
  root: {
    padding: theme.spacing(4)
  },
  card: {
    marginTop: 25
  },
  controls: {
    marginTop: 25,
    display: 'flex',
    justifyContent: 'flex-end',
    p: 2
  },
  control: {
    marginLeft: '25px'
  },
  alert: {
    marginTop: 25
  }
}));

interface UserRouteParams {
  user?: string,
  type?: string
}

const UserEditor: React.FC<RouteComponentProps<UserRouteParams>> = props => {
  const whoAmI = useWhoAmIQuery()
  const [getUserInfo, userInfo] = useUserInfoLazyQuery()

  const classes = useStyles()

  const [user, setUser] = useState('');
  const [human, setHuman] = useState(true);
  const [name, setName] = useState('');
  const [roles, setRoles] = useState(new Array<UserRole>());
  const [oldPassword, setOldPassword] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [email, setEmail] = useState('');

  const [initialized, setInitialized] = useState(false);
  const [error, setError] = useState('');

  const userToEdit = props.match.params.user
  const [byAdmin, setByAdmin] = useState(false);

  if (!initialized && whoAmI.data) {
    setByAdmin(whoAmI.data.whoAmI.roles.find(role => role == UserRole.Administrator) != undefined)
    if (userToEdit) {
      if (!userInfo.data && !userInfo.loading) {
        getUserInfo({variables: {user: userToEdit}})
      }
      if (userInfo.data) {
        const info = userInfo.data.usersInfo[0]
        if (info) {
          setUser(info.user)
          setHuman(info.human)
          setName(info.name)
          setRoles(info.roles)
          if (info.email) setEmail(info.email)
        }
        setInitialized(true)
      }
    } else {
      setHuman(props.match.params.type == 'human')
      setInitialized(true)
    }
  }

  const [addUser] =
    useAddUserMutation({
      onError(err) { console.log(err) }
    })

  const [changeUser] =
    useChangeUserMutation({
      onError(err) { console.log(err) }
    })

  const submit = () => {
    if (!user) {
      setError('User is empty')
    } else if (!name) {
      setError('Name is empty')
    } else if (roles.length == 0) {
      setError('Choose Roles')
    } else if (!userToEdit && !password) {
      setError('Enter password')
    } else if (!byAdmin && password && !oldPassword) {
      setError('Old password is empty')
    } else if (password && password != confirmPassword) {
      setError('Enter same password twice')
    } else {
      if (userToEdit) {
        changeUser({variables: { user: user, name: name,
          oldPassword: oldPassword, password: password, roles: roles, email: email }} ).then( (result) =>
        { if (result.errors) setGraphqlError(result.errors); else window.location.href = '/users' } )
      } else {
        addUser({variables: { user: user, human: human, name: name,
          password: password, roles: roles, email: email }}).then( (result) =>
        { if (result.errors) setGraphqlError(result.errors); else window.location.href = '/users' } )
      }
    }
  }

  const setGraphqlError = (error: ReadonlyArray<GraphQLError>) => {
    let msg = ''
    error.forEach( error => { if (msg) msg += '\n'; msg += error.message } )
    setError(msg)
  }

  const UserCard = () => {
    return (
      <Card className={classes.card}>
        <CardHeader title={userToEdit?'User':(human?'New User':'New Service User')}/>
        <CardContent>
          <TextField
            autoFocus
            fullWidth
            label="User"
            margin="normal"
            value={user}
            onChange={(e: any) => setUser(e.target.value)}
            error={!user}
            disabled={userToEdit !== undefined}
            required
            variant="outlined"
          />
          <TextField
            fullWidth
            label="Name"
            margin="normal"
            value={name}
            onChange={(e: any) => setName(e.target.value)}
            error={!user}
            required
            variant="outlined"
          />
          <TextField
            fullWidth
            label="E-Mail"
            autoComplete="email"
            margin="normal"
            value={email}
            onChange={(e: any) => setEmail(e.target.value)}
            variant="outlined"
          />
        </CardContent>
      </Card>)
  }

  const PasswordCard = () => {
    return (
      <Card className={classes.card}>
        <CardHeader title='Password'/>
        <CardContent>
          { (whoAmI.data && !whoAmI.data.whoAmI.roles.find(role => role == UserRole.Administrator)) ?
            <TextField
              fullWidth
              label="Old Password"
              type="password"
              margin="normal"
              onChange={(e: any) => setOldPassword(e.target.value)}
              required
              variant="outlined"
            /> : null }
          <TextField
            fullWidth
            label="Password"
            type="password"
            margin="normal"
            onChange={(e: any) => setPassword(e.target.value)}
            required
            variant="outlined"
          />
          { password ? <TextField
            fullWidth
            label="Confirm Password"
            type="password"
            margin="normal"
            onChange={(e: any) => setConfirmPassword(e.target.value)}
            helperText={password != confirmPassword ? 'Must be same as password' : ''}
            error={password != confirmPassword}
            required
            variant="outlined"
          /> : null }
        </CardContent>
      </Card>)
  }

  const checkRole = (role: UserRole, checked: boolean) => {
    if (checked) {
      setRoles(previousRoles => {
        if (previousRoles.find(r => r == role)) return previousRoles
        const roles = [...previousRoles]
        roles.push(role)
        return roles
      })
    } else {
      setRoles(previousRoles => {
        return previousRoles.filter(r => r != role)
      })
    }
  }

  const RolesCard = () => {
    return (<Card className={classes.card}>
      <CardHeader title='Roles'/>
      <CardContent>
        <FormGroup row>
          { human ? (
            <>
              <FormControlLabel
                control={(
                  <Checkbox
                    color="primary"
                    checked={roles.find(role => role == UserRole.Developer) != undefined}
                    onChange={ event => checkRole(UserRole.Developer, event.target.checked) }
                  />
                )}
                label="Developer"
              />
              <FormControlLabel
                control={(
                  <Checkbox
                    color="primary"
                    checked={roles.find(role => role == UserRole.Administrator) != undefined}
                    onChange={ event => checkRole(UserRole.Administrator, event.target.checked) }
                  />
                )}
                label="Administrator"
              />
            </> ) : (
            <>
              <FormControlLabel
                control={(
                  <Checkbox
                    color="primary"
                    checked={roles.find(role => role == UserRole.Distribution) != undefined}
                    onChange={ event => checkRole(UserRole.Distribution, event.target.checked) }
                  />
                )}
                label="Distribution"
              />
              <FormControlLabel
                control={(
                  <Checkbox
                    color="primary"
                    checked={roles.find(role => role == UserRole.Builder) != undefined}
                    onChange={ event => checkRole(UserRole.Builder, event.target.checked) }
                  />
                )}
                label="Builder"
              />
              <FormControlLabel
                control={(
                  <Checkbox
                    color="primary"
                    checked={roles.find(role => role == UserRole.Updater) != undefined}
                    onChange={ event => checkRole(UserRole.Updater, event.target.checked) }
                  />
                )}
                label="Updater"
              />
            </>
          ) }
        </FormGroup>
      </CardContent>
    </Card>)
  }

  return (
    initialized ? (
      <Card
        className={clsx(classes.root)}
      >
        {UserCard()}
        <Divider />
        {RolesCard()}
        <Divider />
        {PasswordCard()}
        {error && <Alert className={classes.alert} severity='error'>{error}</Alert>}
        <Box className={classes.controls}>
          <Button className={classes.control}
            color="primary"
            variant="contained"
            href='/users'
          >
            Cancel
          </Button>
          <Button className={classes.control}
            color="primary"
            variant="contained"
            onClick={() => submit()}
          >
            {!userToEdit?'Add New User':'Save'}
          </Button>
        </Box>
      </Card>) : null
  );
}

export default UserEditor;
