import React, {useState} from 'react';

import Button from '@material-ui/core/Button';
import TextField from '@material-ui/core/TextField';
import Alert from '@material-ui/lab/Alert';

import { makeStyles } from '@material-ui/core/styles';
import {Box, Card, CardContent, CardHeader, Divider} from '@material-ui/core';
import {useChangeUserMutation, UserInfo} from '../../../../generated/graphql';
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

interface UserEditorProps {
  byAdmin: boolean,
  userInfo: UserInfo|undefined
}

const UserEditor: React.FC<UserEditorProps> = props => {
  const { byAdmin, userInfo } = props
  const classes = useStyles()

  const [user, setUser] = useState(userInfo?.user);
  const [name, setName] = useState(userInfo?.name);
  const [email, setEmail] = useState(userInfo?.email);
  const [oldPassword, setOldPassword] = useState('');
  const [password, setPassword] = useState('');
  const [roles, setRoles] = useState(userInfo?.roles);

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
            onChange={(e: any) => setUser(e.target.value)}
            required
            variant="outlined"
          />
        </CardContent>
        <CardContent className={classes.content}>
          <TextField
            fullWidth
            label="Name"
            margin="normal"
            onChange={(e: any) => setName(e.target.value)}
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
            onChange={(e: any) => setEmail(e.target.value)}
            required
            variant="outlined"
          />
        </CardContent>
      </Card>)
  }

  const PasswordCard = () => {
    return (
      <Card>
        <CardContent className={classes.content}>
          <TextField
            fullWidth
            label="Old Password"
            margin="normal"
            onChange={(e: any) => setOldPassword(e.target.value)}
            required
            variant="outlined"
          /> : null
          <TextField
            fullWidth
            label="Password"
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
        title={props.userInfo?`User ${props.userInfo.user}`:'New user'}
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
