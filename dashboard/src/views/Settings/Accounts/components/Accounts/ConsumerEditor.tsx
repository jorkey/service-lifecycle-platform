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
  AccountRole, ConsumerAccountProperties,
  useAccountsListQuery, useAddConsumerAccountMutation,
  useAddUserAccountMutation, useChangeConsumerAccountMutation,
  useChangeUserAccountMutation, useConsumerAccountInfoLazyQuery, UserAccountProperties,
  useServiceProfilesQuery,
  useUserAccountInfoLazyQuery,
  useWhoAmIQuery
} from '../../../../../generated/graphql';
import clsx from 'clsx';
import Alert from "@material-ui/lab/Alert";

const useStyles = makeStyles(theme => ({
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

const ConsumerEditor: React.FC<AccountEditorParams> = props => {
  const whoAmI = useWhoAmIQuery({
    fetchPolicy: 'no-cache', // base option no-cache does not work
  })
  const {data: accountsList} = useAccountsListQuery({
    fetchPolicy: 'no-cache', // base option no-cache does not work
  })
  const {data: profilesList} = useServiceProfilesQuery({
    fetchPolicy: 'no-cache', // base option no-cache does not work
  })
  const [getAccountInfo, accountInfo] = useConsumerAccountInfoLazyQuery({
    fetchPolicy: 'no-cache', // base option no-cache does not work
  })

  const classes = useStyles()

  const [account, setAccount] = useState<string>();
  const [name, setName] = useState<string>();
  const [url, setUrl] = useState<string>();
  const [profile, setProfile] = useState<string>();

  const [initialized, setInitialized] = useState(false);

  const editAccount = props.match.params.account

  if (!initialized && whoAmI.data) {
    if (editAccount) {
      if (!accountInfo.data && !accountInfo.loading) {
        getAccountInfo({variables: {account: editAccount}})
      }
      if (accountInfo.data) {
        const info = accountInfo.data.consumerAccountsInfo[0]
        if (info) {
          setAccount(info.account)
          setName(info.name)
          setUrl(info.properties.url)
          setProfile(info.properties.profile)
        }
        setInitialized(true)
      }
    } else {
      setInitialized(true)
    }
  }

  const [addAccount, { data: addAccountData, error: addAccountError }] =
    useAddConsumerAccountMutation({
      onError(err) { console.log(err) }
    })

  const [changeAccount, { data: changeAccountData, error: changeAccountError }] =
    useChangeConsumerAccountMutation({
      onError(err) { console.log(err) }
    })

  if (addAccountData || changeAccountData) {
    return <Redirect to={props.fromUrl}/>
  }

  const validate: () => boolean = () => {
    return  !!account && !!name &&
            !!profile && !!url && validateUrl(url) &&
            (!!editAccount || !doesAccountExist(account))
  }

  const validateUrl = (url: string) => {
    try { new URL(url); return true } catch { return false }
  }

  const submit = () => {
    if (validate()) {
      const properties: ConsumerAccountProperties = { url: url!, profile: profile! }
      if (editAccount) {
          changeAccount({
            variables: {
              account: account!, name: name,
              properties: properties
            }
          })
      } else {
        addAccount({
          variables: { account: account!, name: name!, properties: properties }
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
        <CardHeader title={(editAccount?'Edit ':'New Distribution Consumer') +
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
              label="URL"
              margin="normal"
              value={ url ? url : '' }
              onChange={(e: any) => setUrl(e.target.value)}
              variant="outlined"
          />
          <FormGroup row className={classes.profile}>
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
          </FormGroup>
        </CardContent>
      </Card>)
  }

  const error = addAccountError?addAccountError.message:changeAccountError?changeAccountError.message:''

  return (
    initialized ? (
      <Card>
        {AccountCard()}
        <Divider />
        <Divider />
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

export default ConsumerEditor;
