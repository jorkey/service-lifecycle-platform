import React, {useState} from 'react';

import Button from '@material-ui/core/Button';
import TextField from '@material-ui/core/TextField';
import Alert from '@material-ui/lab/Alert';

import { makeStyles } from '@material-ui/core/styles';
import {Card, CardContent, CardHeader, Divider} from '@material-ui/core';
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
  const classes = useStyles()

  const [user, setUser] = useState(props.userInfo?.user);
  const [oldPassword, setOldPassword] = useState('');
  const [password, setPassword] = useState('');
  const [roles, setRoles] = useState(props.userInfo?.roles);
  const [human, setHuman] = useState(props.userInfo?.human);

  const [changeUser, { loading, error }] =
    useChangeUserMutation({
      onError(err) { console.log(err) }
    })

  const handleSubmit = (e: any) => {
    e.preventDefault();
    if (user) {
      changeUser({variables: {user: user, oldPassword: oldPassword, password: password, roles: roles, human: human}} )
    }
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
        <form
          name="form"
          onSubmit={handleSubmit}
        >
          <TextField
            autoComplete="email"
            autoFocus={props.userInfo != undefined}
            defaultValue={props.userInfo}
            disabled={props.userInfo != undefined}
            fullWidth
            id="user"
            label="User Name"
            margin="normal"
            name="user"
            onChange={(e: any) => setUser(e.target.value)}
            required
            variant="outlined"
          />
          { (!props.byAdmin && props.userInfo) ?
            <TextField
              fullWidth
              id="oldPassword"
              label="Old Password"
              margin="normal"
              name="oldPassword"
              onChange={(e: any) => setOldPassword(e.target.value)}
              required
              type="password"
              variant="outlined"
            /> : null
          }
          <TextField
            fullWidth
            id="password"
            label="Password"
            margin="normal"
            name="password"
            onChange={(e: any) => setPassword(e.target.value)}
            required
            type="password"
            variant="outlined"
          />
          <Button
            color="primary"
            disabled={loading}
            fullWidth
            type="submit"
            variant="contained"
          >
            Sign In
          </Button>
          {error && <Alert severity="error">{error.message}</Alert>}
        </form>
      </CardContent>
    </Card>
  );
}

export default UserEditor;
