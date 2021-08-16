import React, {useState} from 'react';

import Button from '@material-ui/core/Button';
import TextField from '@material-ui/core/TextField';
import {NavLink as RouterLink, RouteComponentProps, useHistory} from "react-router-dom"

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
  AccountRole, ConsumerInfoInput, HumanInfoInput,
  useAccountInfoLazyQuery,
  useAccountsListQuery,
  useAddAccountMutation,
  useChangeAccountMutation, useServiceProfilesQuery,
  useWhoAmIQuery
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
  url: {
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
  type: 'human' | 'service' | 'consumer',
  account?: string
}

interface AccountEditorParams extends RouteComponentProps<AccountRouteParams> {
  fromUrl: string
}

const AccountEditor: React.FC<AccountEditorParams> = props => {
  const whoAmI = useWhoAmIQuery()
  const {data: accountsList} = useAccountsListQuery()
  const {data: profilesList} = useServiceProfilesQuery()
  const [getAccountInfo, accountInfo] = useAccountInfoLazyQuery()

  const classes = useStyles()

  const [account, setAccount] = useState<string>();
  const [name, setName] = useState<string>();
  const [roles, setRoles] = useState(new Array<AccountRole>());
  const [changePassword, setChangePassword] = useState(false);
  const [oldPassword, setOldPassword] = useState();
  const [password, setPassword] = useState<string>();
  const [confirmPassword, setConfirmPassword] = useState<string>();
  const [email, setEmail] = useState<string>();
  const [profile, setProfile] = useState<string>();
  const [url, setUrl] = useState<string>();

  const [initialized, setInitialized] = useState(false);

  const editAccount = props.match.params.account
  const [byAdmin, setByAdmin] = useState(false)

  const accountType = props.match.params.type

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
          setName(info.name)
          setRoles(info.roles)
          if (info.human) {
            setEmail(info.human.email)
          } else if (info.consumer) {
            setProfile(info.consumer.profile)
            setUrl(info.consumer.url)
          }
        }
        setByAdmin(whoAmI.data.whoAmI.roles.find(role => role == AccountRole.Administrator) != undefined)
        setInitialized(true)
      }
    } else {
      setByAdmin(whoAmI.data.whoAmI.roles.find(role => role == AccountRole.Administrator) != undefined)
      if (accountType == 'consumer') {
        setRoles([AccountRole.Consumer])
      }
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
            ((accountType == 'human') ? !!email : (accountType == 'consumer') ? !!profile && !!url && validateUrl(url) : true) &&
            (!!editAccount || !doesAccountExist(account)) &&
            (!!editAccount || !!password) &&
            (byAdmin || !!oldPassword) &&
            (!password || password == confirmPassword)
  }

  const submit = () => {
    if (validate()) {
      const human: HumanInfoInput | undefined = (accountType == 'human')?{ email: email!, notifications: [] }:undefined
      const consumer: ConsumerInfoInput | undefined = (accountType == 'consumer')?{ profile: profile!, url: url! }:undefined
      if (editAccount) {
          changeAccount({
            variables: {
              account: account!, name: name, oldPassword: oldPassword, password: password, roles: roles,
              human: human, consumer: consumer
            }
          })
      } else {
        addAccount({variables: { account: account!, name: name!, password: password!, roles: roles,
          human: human, consumer: consumer }})
      }
    }
  }

  const doesAccountExist: (account: string) => boolean = (account) => {
    return accountsList?!!accountsList.accountsInfo.find(info => info.account == account):false
  }

  const AccountCard = () => {
    return (
      <Card className={classes.card}>
        <CardHeader title={(editAccount?'Edit ':'New ') +
        (accountType=='human'?'Operator':accountType=='service'?'Service':accountType=='consumer'?'Consumer':'') +
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
          { accountType == 'human' ?
            (<TextField
              fullWidth
              label="E-Mail"
              autoComplete="email"
              margin="normal"
              value={ email ? email : '' }
              onChange={(e: any) => setEmail(e.target.value)}
              variant="outlined"
            />) : accountType == 'consumer' ?
              (<FormGroup row className={classes.profile}>
                <Typography className={classes.profileTitle}>Services Profile</Typography>
                <Select className={classes.profileSelect}
                        native
                        value={ profile ? profile : '' }
                        error={ !profile }
                        onChange={(e: any) => { if (e.target.value) setProfile(e.target.value as string); else setProfile(undefined) }}
                >
                  {
                    profilesList?.serviceProfiles?
                      [<option key='0'></option>,
                        ...profilesList?.serviceProfiles.map(profile => <option key={profile.profile}>{profile.profile}</option>)]:null
                  }
                </Select>
                <TextField
                  className={classes.url}
                  fullWidth
                  label='URL'
                  margin='normal'
                  value={ url ? url : '' }
                  error={ !url || !validateUrl(url) }
                  onChange={(e: any) => setUrl(e.target.value)}
                  variant='outlined'
                />
              </FormGroup>) : null
          }
        </CardContent>
      </Card>)
  }

  const validateUrl = (url: string) => {
    try { new URL(url); return true } catch { return false }
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
    return accountType != 'consumer' ? (<Card className={classes.card}>
      <CardHeader title='Roles'/>
      <CardContent>
        <FormGroup row>
          { accountType == 'human' ? (
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
            </> ) : accountType == 'service' ? (
            <>
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
          ) : null }
        </FormGroup>
      </CardContent>
    </Card>) : null
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
