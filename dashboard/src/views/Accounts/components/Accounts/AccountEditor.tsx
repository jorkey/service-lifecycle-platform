import React, {useState} from 'react';

import Button from '@material-ui/core/Button';
import TextField from '@material-ui/core/TextField';
import {NavLink as RouterLink, RouteComponentProps, useRouteMatch, useHistory} from "react-router-dom"

import { makeStyles } from '@material-ui/core/styles';
import {Box, Card, CardContent, CardHeader, Checkbox, Divider, FormControlLabel, FormGroup} from '@material-ui/core';
import {
  useAddAccountMutation,
  useChangeAccountMutation,
  AccountRole,
  useAccountInfoLazyQuery, useAccountsListQuery, useWhoAmIQuery
} from '../../../../generated/graphql';
import clsx from 'clsx';
import Alert from "@material-ui/lab/Alert";

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
    marginLeft: '25px',
    textTransform: 'none'
  },
  alert: {
    marginTop: 25
  }
}));

interface AccountRouteParams {
  type: string,
  account?: string
}

interface AccountEditorParams extends RouteComponentProps<AccountRouteParams> {
  fromUrl: string
}

const AccountEditor: React.FC<AccountEditorParams> = props => {
  const whoAmI = useWhoAmIQuery()
  const {data: accountsList} = useAccountsListQuery()
  const [getAccountInfo, accountInfo] = useAccountInfoLazyQuery()

  const classes = useStyles()

  const [account, setAccount] = useState('');
  const [human, setHuman] = useState(true);
  const [name, setName] = useState('');
  const [roles, setRoles] = useState(new Array<AccountRole>());
  const [changePassword, setChangePassword] = useState(false);
  const [oldPassword, setOldPassword] = useState<string>();
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [email, setEmail] = useState('');

  const [initialized, setInitialized] = useState(false);

  const editAccount = props.match.params.account
  const [byAdmin, setByAdmin] = useState(false);

  const history = useHistory()

  if (!initialized && whoAmI.data) {
    if (editAccount) {
      if (!accountInfo.data && !accountInfo.loading) {
        getAccountInfo({variables: {account: editAccount}})
      }
      if (accountInfo.data) {
        const info = accountInfo.data.accountsInfo[0]
        if (info) {
          setAccount(info.account)
          setHuman(info.human)
          setName(info.name)
          setRoles(info.roles)
          if (info.email) setEmail(info.email)
        }
        setByAdmin(whoAmI.data.whoAmI.roles.find(role => role == AccountRole.Administrator) != undefined)
        setInitialized(true)
      }
    } else {
      setHuman(props.match.params.type == 'human')
      setByAdmin(whoAmI.data.whoAmI.roles.find(role => role == AccountRole.Administrator) != undefined)
      setInitialized(true)
    }
  }

  const [addAccount, { data: addAccountData, error: addAccountError }] =
    useAddAccountMutation({
      onError(err) { console.log(err) }
    })

  const [changeAccount, { data: changeAccountData, error: changeAccountError }] =
    useChangeAccountMutation({
      onError(err) { console.log(err) }
    })

  if (addAccountData || changeAccountData) {
    history.push(props.fromUrl + '/' + props.match.params.type)
  }

  const validate: () => boolean = () => {
    return  !!account && !!name && roles.length != 0 &&
            (!!editAccount || !doesAccountExist(account)) &&
            (!!editAccount || !!password) &&
            (byAdmin || !!oldPassword) &&
            (!password || password == confirmPassword)
  }

  const submit = () => {
    if (validate()) {
      if (editAccount) {
        changeAccount({variables: { account: account, name: name, oldPassword: oldPassword, password: password, roles: roles, email: email }} )
      } else {
        addAccount({variables: { account: account, human: human, name: name, password: password, roles: roles, email: email }})
      }
    }
  }

  const doesAccountExist: (account: string) => boolean = (account) => {
    return accountsList?!!accountsList.accountsInfo.find(info => info.account == account):false
  }

  const AccountCard = () => {
    return (
      <Card className={classes.card}>
        <CardHeader title={editAccount?'Edit Account':(human?'New Account':'New Service Account')}/>
        <CardContent>
          <TextField
            autoFocus
            fullWidth
            label="Account"
            margin="normal"
            value={account}
            helperText={!editAccount && doesAccountExist(account) ? 'Account already exists': ''}
            error={!account || (!editAccount && doesAccountExist(account))}
            onChange={(e: any) => setAccount(e.target.value)}
            disabled={editAccount !== undefined}
            required
            variant="outlined"
          />
          <TextField
            fullWidth
            label="Name"
            margin="normal"
            value={name}
            onChange={(e: any) => setName(e.target.value)}
            error={!account}
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
    if (whoAmI.data) {
      const admin = whoAmI.data.whoAmI.roles.find(role => role == AccountRole.Administrator)
      const self = whoAmI.data.whoAmI.account == editAccount
      return (
        <Card className={classes.card}>
          <CardHeader title='Password'/>
          <CardContent>
            { (editAccount && !changePassword)?
              <Button
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

  const checkRole = (role: AccountRole, checked: boolean) => {
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
                    checked={roles.find(role => role == AccountRole.Developer) != undefined}
                    onChange={ event => checkRole(AccountRole.Developer, event.target.checked) }
                  />
                )}
                label="Developer"
              />
              <FormControlLabel
                control={(
                  <Checkbox
                    color="primary"
                    checked={roles.find(role => role == AccountRole.Administrator) != undefined}
                    onChange={ event => checkRole(AccountRole.Administrator, event.target.checked) }
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
                    checked={roles.find(role => role == AccountRole.Distribution) != undefined}
                    onChange={ event => checkRole(AccountRole.Distribution, event.target.checked) }
                  />
                )}
                label="Distribution"
              />
              <FormControlLabel
                control={(
                  <Checkbox
                    color="primary"
                    checked={roles.find(role => role == AccountRole.Builder) != undefined}
                    onChange={ event => checkRole(AccountRole.Builder, event.target.checked) }
                  />
                )}
                label="Builder"
              />
              <FormControlLabel
                control={(
                  <Checkbox
                    color="primary"
                    checked={roles.find(role => role == AccountRole.Updater) != undefined}
                    onChange={ event => checkRole(AccountRole.Updater, event.target.checked) }
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
            to={props.fromUrl + '/' + props.match.params.type}
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

export default AccountEditor;
