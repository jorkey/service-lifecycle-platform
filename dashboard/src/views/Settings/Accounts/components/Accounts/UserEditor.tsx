import React, {useState} from 'react';

import Button from '@material-ui/core/Button';
import TextField from '@material-ui/core/TextField';
import {NavLink as RouterLink, Redirect, RouteComponentProps, useHistory} from "react-router-dom"

import {makeStyles} from '@material-ui/core/styles';
import {
  Box,
  Card,
  CardContent,
  CardHeader,
  Checkbox,
  Divider,
  FormControlLabel,
  FormGroup,
  Select, Typography
} from '@material-ui/core';
import {
  AccountRole,
  useAccountsListQuery,
  useAddUserAccountMutation,
  useChangeUserAccountMutation, UserAccountProperties,
  useUserAccountInfoLazyQuery,
  useWhoAmIQuery
} from '../../../../../generated/graphql';
import clsx from 'clsx';
import Alert from "@material-ui/lab/Alert";

const useStyles = makeStyles(theme => ({
  root: {
    padding: theme.spacing(4)
  },
  card: {
    marginTop: 25
  },
  profile: {
    marginBottom: 20
  },
  profileTitle: {
    marginTop: 5,
    height: 25,
    marginRight: 15
  },
  profileSelect: {
    width: '100%',
    height: 25
  },
  controls: {
    marginTop: 25,
    display: 'flex',
    justifyContent: 'flex-end',
    p: 2
  },
  control: {
    marginLeft: '25px',
    textTransform: 'none'
  },
  alert: {
    marginTop: 25
  }
}));

interface AccountRouteParams {
  account?: string
}

interface AccountEditorParams extends RouteComponentProps<AccountRouteParams> {
  fromUrl: string
}

const UserEditor: React.FC<AccountEditorParams> = props => {
  const whoAmI = useWhoAmIQuery({
    fetchPolicy: 'no-cache', // base option no-cache does not work
  })
  const {data: accountsList} = useAccountsListQuery({
    fetchPolicy: 'no-cache', // base option no-cache does not work
  })
  const [getAccountInfo, accountInfo] = useUserAccountInfoLazyQuery({
    fetchPolicy: 'no-cache', // base option no-cache does not work
  })

  const classes = useStyles()

  const [account, setAccount] = useState<string>();
  const [name, setName] = useState<string>();
  const [role, setRole] = useState<AccountRole>();
  const [changePassword, setChangePassword] = useState(false);
  const [oldPassword, setOldPassword] = useState();
  const [password, setPassword] = useState<string>();
  const [confirmPassword, setConfirmPassword] = useState<string>();
  const [email, setEmail] = useState<string>();

  const [initialized, setInitialized] = useState(false);

  const editAccount = props.match.params.account
  const [byAdmin, setByAdmin] = useState(false)

  if (!initialized && whoAmI.data) {
    if (editAccount) {
      if (!accountInfo.data && !accountInfo.loading) {
        getAccountInfo({variables: {account: editAccount}})
      }
      if (accountInfo.data) {
        const info = accountInfo.data.userAccountsInfo[0]
        if (info) {
          setAccount(info.account)
          setName(info.name)
          setRole(info.role)
          setEmail(info.properties.email?info.properties.email:undefined)
        }
        setByAdmin(whoAmI.data.whoAmI.role == AccountRole.Administrator)
        setInitialized(true)
      }
    } else {
      setByAdmin(whoAmI.data.whoAmI.role == AccountRole.Administrator)
      setInitialized(true)
    }
  }

  const [addAccount, { data: addAccountData, error: addAccountError }] =
    useAddUserAccountMutation({
      onError(err) { console.log(err) }
    })

  const [changeAccount, { data: changeAccountData, error: changeAccountError }] =
    useChangeUserAccountMutation({
      onError(err) { console.log(err) }
    })

  if (addAccountData || changeAccountData) {
    return <Redirect to={props.fromUrl}/>
  }

  const validate: () => boolean = () => {
    return  !!account && !!name && !!role &&
            (!!editAccount || !doesAccountExist(account)) &&
            (!!editAccount || !!password) &&
            (byAdmin || !!oldPassword) &&
            (!password || password == confirmPassword)
  }

  const submit = () => {
    if (validate()) {
      const properties: UserAccountProperties = { email: email!, notifications: [] }
      if (editAccount) {
          changeAccount({
            variables: {
              account: account!, name: name, role: role!, oldPassword: oldPassword, password: password,
              properties: properties
            }
          })
      } else {
        addAccount({
          variables: { account: account!, name: name!, role: role!, password: password!, properties: properties }
        })
      }
    }
  }

  const doesAccountExist: (account: string) => boolean = (account) => {
    return accountsList?!!accountsList.accountsList.find(acc => acc == account):false
  }

  const AccountCard = () => {
    return (
      <Card className={classes.card}>
        <CardHeader title={(editAccount?'Edit ':'New User') +
          ' Account' + (editAccount? ` '${account}'`:'')}/>
        <CardContent>
          { !editAccount?
            <TextField
              autoFocus
              fullWidth
              label="Account"
              margin="normal"
              value={account?account:''}
              helperText={!editAccount && account && doesAccountExist(account) ? 'Account already exists': ''}
              error={!account || (!editAccount && doesAccountExist(account))}
              onChange={(e: any) => setAccount(e.target.value)}
              disabled={editAccount !== undefined}
              required
              variant="outlined"
            />:null }
          <TextField
            fullWidth
            label="Name"
            margin="normal"
            value={name?name:''}
            onChange={(e: any) => setName(e.target.value)}
            error={!name}
            required
            variant="outlined"
            autoComplete="off"
          />
          <TextField
              fullWidth
              label="E-Mail"
              autoComplete="email"
              margin="normal"
              value={ email ? email : '' }
              onChange={(e: any) => setEmail(e.target.value)}
              variant="outlined"
          />
        </CardContent>
      </Card>)
  }

  const PasswordCard = () => {
    if (whoAmI.data) {
      const admin = whoAmI.data.whoAmI.role == AccountRole.Administrator
      const self = whoAmI.data.whoAmI.account == editAccount
      return (
        <Card className={classes.card}>
          <CardHeader title='Password'/>
          <CardContent>
            { (editAccount && !changePassword)?
              <Button className={classes.control}
                color="primary"
                variant="contained"
                onClick={ () => setChangePassword(true) }
              >Change Password</Button> : null }
            { (self && changePassword) ?
              <TextField
                fullWidth
                label="Old Password"
                type="password"
                margin="normal"
                onChange={(e: any) => setOldPassword(e.target.value)}
                required
                variant="outlined"
              /> : null}
            { (!editAccount || changePassword) ?
              <>
                <TextField
                  fullWidth
                  label="Password"
                  type="password"
                  margin="normal"
                  onChange={(e: any) => setPassword(e.target.value)}
                  required
                  variant="outlined"
                />
                {password ? <TextField
                  fullWidth
                  label="Confirm Password"
                  type="password"
                  margin="normal"
                  onChange={(e: any) => setConfirmPassword(e.target.value)}
                  helperText={password != confirmPassword ? 'Must be same as password' : ''}
                  error={password != confirmPassword}
                  required
                  variant="outlined"
                /> : null}
              </> : null }
          </CardContent>
        </Card>)
    } else {
      return null
    }
  }

  const RolesCard = () => {
    return <Card className={classes.card}>
      <CardHeader title='Roles'/>
      <CardContent>
        <FormGroup row>
          <FormControlLabel
            control={(
              <Checkbox
                color="primary"
                checked={role == AccountRole.Developer}
                onChange={ event => setRole(event.target.checked ? AccountRole.Developer : undefined) }
              />
            )}
            label="Developer"
          />
          <FormControlLabel
            control={(
              <Checkbox
                color="primary"
                checked={role == AccountRole.Administrator}
                onChange={ event => setRole(event.target.checked ? AccountRole.Administrator : undefined) }
              />
            )}
            label="Administrator"
          />
        </FormGroup>
      </CardContent>
    </Card>
  }

  const error = addAccountError?addAccountError.message:changeAccountError?changeAccountError.message:''

  return (
    initialized ? (
      <Card
        className={clsx(classes.root)}
      >
        {AccountCard()}
        <Divider />
        {RolesCard()}
        <Divider />
        {PasswordCard()}
        {error && <Alert className={classes.alert} severity='error'>{error}</Alert>}
        <Box className={classes.controls}>
          <Button className={classes.control}
            color="primary"
            variant="contained"
            component={RouterLink}
            to={props.fromUrl}
          >
            Cancel
          </Button>
          <Button className={classes.control}
            color="primary"
            variant="contained"
            disabled={!validate()}
            onClick={() => submit()}
          >
            {!editAccount?'Add New Account':'Save'}
          </Button>
        </Box>
      </Card>) : null
  );
}

export default UserEditor;
