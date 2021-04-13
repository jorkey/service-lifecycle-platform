import React, {useState} from 'react';

import Button from '@material-ui/core/Button';
import TextField from '@material-ui/core/TextField';
import { RouteComponentProps } from "react-router-dom"

import { makeStyles } from '@material-ui/core/styles';
import {Box, Card, CardContent, CardHeader, Divider} from '@material-ui/core';
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
  user: string|undefined
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
  const [email, setEmail] = useState<string|null>();

  if (props.match.params.user) {
    getUserInfo({ variables: { user: props.match.params.user } })
  }
  if (userInfo) {
    const info = userInfo.data?.usersInfo[0]
    if (info) {
      setUser(info.user)
      setName(info.name)
      setRoles(info.roles)
      setEmail(info.email)
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
        <CardContent className={classes.content}>
          <TextField
            autoFocus
            fullWidth
            label="User"
            margin="normal"
            value={user}
            onChange={(e: any) => setUser(e.target.value)}
            error={!user}
            required
            variant="outlined"
          />
        </CardContent>
        <CardContent className={classes.content}>
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
        </CardContent>
        <CardContent className={classes.content}>
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
        <CardContent className={classes.content}>
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

  return (
    <Card
      className={clsx(classes.root)}
    >
      <CardHeader
        title='User'
      />
      <Divider />
      <CardContent className={classes.content}>
        <UserCard/>
        <PasswordCard/>
      </CardContent>
      <Divider/>
      <Box>
        <Button
          color="primary"
          variant="contained"
        >
          Save details
        </Button>
      </Box>
    </Card>
  );
}

export default UserEditor;
