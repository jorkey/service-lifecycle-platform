import React, {useState} from 'react';

import Button from '@material-ui/core/Button';
import TextField from '@material-ui/core/TextField';
import { RouteComponentProps } from "react-router-dom"

import { makeStyles } from '@material-ui/core/styles';
import {Box, Card, CardContent, CardHeader, Checkbox, Divider, FormControlLabel} from '@material-ui/core';
import {
  useChangeUserMutation,
  UserRole,
  useUserInfoLazyQuery, useWhoAmIQuery
} from '../../../../generated/graphql';
import clsx from 'clsx';

const useStyles = makeStyles(theme => ({
  root: {
    padding: theme.spacing(4)
  },
  content: {
    paddingTop: 150,
    textAlign: 'center'
  }
}));

interface UserRouteParams {
  user?: string
}

const UserEditor: React.FC<RouteComponentProps<UserRouteParams>> = props => {
  const whoAmI = useWhoAmIQuery()
  const [getUserInfo, userInfo] = useUserInfoLazyQuery()

  const classes = useStyles()

  const [user, setUser] = useState('');
  const [name, setName] = useState('');
  const [roles, setRoles] = useState(new Array<UserRole>());
  const [oldPassword, setOldPassword] = useState('');
  const [password, setPassword] = useState('');
  const [email, setEmail] = useState('');

  const [initialized, setInitialized] = useState(false);

  const userToEdit = props.match.params.user

  if (!initialized) {
    if (userToEdit) {
      if (!userInfo.data && !userInfo.loading) {
        getUserInfo({variables: {user: userToEdit}})
      }
      if (userInfo.data) {
        const info = userInfo.data.usersInfo[0]
        if (info) {
          setUser(info.user)
          setName(info.name)
          setRoles(info.roles)
          if (info.email) setEmail(info.email)
        }
        setInitialized(true)
      }
    } else {
      setInitialized(true)
    }
  }

  const [changeUser] =
    useChangeUserMutation({
      onError(err) { console.log(err) }
    })

  const handleSubmit = (e: any) => {
    e.preventDefault();
    if (user && name) {
      changeUser({variables: { user: user, name: name,
          oldPassword: oldPassword, password: password, roles: roles, email: email }} )
    }
  }

  const UserCard = () => {
    return (
      <Card>
        <CardHeader title='User'/>
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
      <Card>
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
          <TextField
            fullWidth
            label="Confirm Password"
            type="password"
            margin="normal"
            onChange={(e: any) => setPassword(e.target.value)}
            required
            variant="outlined"
          />
        </CardContent>
      </Card>)
  }

  const RolesCard = () => {
    return (<Card>
      <CardHeader title='Roles'/>
      <CardContent>
        <FormControlLabel
          control={(
            <Checkbox
              color="primary"
              checked={roles.find(role => role == UserRole.Developer) != undefined}
            />
          )}
          label="Developer"
        />
      </CardContent>
      <CardContent>
        <FormControlLabel
          control={(
            <Checkbox
              color="primary"
              checked={roles.find(role => role == UserRole.Administrator) != undefined}
            />
          )}
          label="Administrator"
        />
      </CardContent>
    </Card>)
  }

  return (
    initialized ? (
      <Card
        className={clsx(classes.root)}
      >
        {/*<CardHeader title='User'/>*/}
        {/*<Divider />*/}
        {/*<CardContent className={classes.content}>*/}
        <Divider />
        {UserCard()}
        <Divider />
        {RolesCard()}
        <Divider />
        {PasswordCard()}
        <Box>
          <Button
            color="primary"
            variant="contained"
          >
            Save details
          </Button>
        </Box>
      </Card>) : null
  );
}

export default UserEditor;
