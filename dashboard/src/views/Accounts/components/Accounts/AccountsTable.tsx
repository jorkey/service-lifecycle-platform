import React, {useState} from 'react';
import {makeStyles} from '@material-ui/styles';
import {AccountRole, useAccountsInfoQuery, useRemoveAccountMutation} from '../../../../generated/graphql';
import DeleteIcon from '@material-ui/icons/Delete';
import {Redirect, useRouteMatch} from "react-router-dom";
import ConfirmDialog from "../../../../common/ConfirmDialog";
import GridTable from "../../../../common/components/gridTable/GridTable";
import Alert from "@material-ui/lab/Alert";
import {GridTableColumnParams, GridTableColumnValue} from "../../../../common/components/gridTable/GridTableColumn";
import {Button} from "@material-ui/core";

const useStyles = makeStyles(theme => ({
  accountsTable: {
  },
  accountColumn: {
    width: '200px',
    padding: '4px',
    paddingLeft: '16px'
  },
  nameColumn: {
    width: '200px',
    padding: '4px',
    paddingLeft: '16px'
  },
  rolesColumn: {
    width: '300px',
    padding: '4px'
  },
  emailColumn: {
    padding: '4px',
    paddingLeft: '16px'
  },
  profileColumn: {
    padding: '4px',
    paddingLeft: '16px'
  },
  urlColumn: {
    padding: '4px',
    paddingLeft: '16px'
  },
  actionsColumn: {
    width: '200px',
    padding: '4px',
    paddingRight: '40px',
    textAlign: 'right'
  },
  alert: {
    marginTop: 25
  }
}));

interface AccountsTableProps {
  accountType: 'human' | 'service' | 'consumer'
}

const AccountsTable: React.FC<AccountsTableProps> = props => {
  const { accountType } = props
  const [ startEdit, setStartEdit ] = React.useState('')
  const [ deleteConfirm, setDeleteConfirm ] = useState('')

  const classes = useStyles()

  const [error, setError] = useState<string>()

  const { data: accountsInfo, refetch: getAccountsInfo } = useAccountsInfoQuery({
    fetchPolicy: 'no-cache',
    onError(err) { setError('Query accounts info error ' + err.message) },
    onCompleted() { setError(undefined) }
  })

  const [removeAccount] = useRemoveAccountMutation({
    onError(err) { setError('Remove account error ' + err.message) },
    onCompleted() { setError(undefined) }
  })

  const routeMatch = useRouteMatch();

  if (startEdit) {
    return <Redirect to={`${routeMatch.url}/edit/${startEdit}`}/>
  }

  const columns: Array<GridTableColumnParams> = [
    {
      name: 'account',
      headerName: 'Account',
      className: classes.accountColumn
    },
    {
      name: 'name',
      headerName: 'Name',
      className: classes.nameColumn
    },
    {
      name: 'roles',
      headerName: 'Roles',
      className: classes.rolesColumn
    }
  ]

  if (accountType == 'human') {
    columns.push({
      name: 'email',
      headerName: 'E-Mail',
      className: classes.emailColumn
    })
  } else if (accountType == 'consumer') {
    columns.push({
      name: 'profile',
      headerName: 'Profile',
      className: classes.profileColumn
    },
    {
      name: 'url',
      headerName: 'URL',
      className: classes.urlColumn
    })
  }

  columns.push({
    name: 'actions',
    headerName: 'Actions',
    type: 'elements',
    className: classes.actionsColumn
  })

  const rows = new Array<Map<string, GridTableColumnValue>>()
  if (accountsInfo) {
    [...accountsInfo.accountsInfo]
      .filter(account => {
        if (accountType == 'human') {
          return account.roles.find(role => { return role == AccountRole.Administrator || role == AccountRole.Developer})
        } else if (accountType == 'service') {
          return account.roles.find(role => { return role == AccountRole.Builder || role == AccountRole.Updater})
        } else if (accountType == 'consumer') {
          return account.roles.find(role => { return role == AccountRole.Consumer })
        } else {
          return false
        }
      })
      .sort((u1,u2) =>  (u1.account > u2.account ? 1 : -1))
      .forEach(account => {
        const row = new Map<string, GridTableColumnValue>()
        row.set('account', account.account)
        row.set('name', account.name)
        row.set('roles', account.roles.toString())
        if (accountType == 'human' && account.human) {
          row.set('email', account.human.email)
        } else if (accountType == 'consumer') {
          row.set('profile', account.consumer!.profile)
          row.set('url', account.consumer!.url)
        }
        row.set('actions', [<Button key='0' onClick={ () => setDeleteConfirm(account.account) }>
            <DeleteIcon/>
          </Button>])
        rows.push(row)
      })
  }

  return (<>
    <GridTable
      className={classes.accountsTable}
      columns={columns}
      rows={rows}
      onClick={ (row, values) =>
        setStartEdit(values.get('account')! as string) }
    />
    {error && <Alert className={classes.alert} severity="error">{error}</Alert>}
    { deleteConfirm ? (
      <ConfirmDialog
        message={`Do you want to delete account '${deleteConfirm}'?`}
        open={true}
        close={() => {
          setDeleteConfirm('')
        }}
        onConfirm={() => {
          removeAccount({ variables: { account: deleteConfirm } }).then(() => getAccountsInfo())
          setDeleteConfirm('')
        }}
      />) : null }
  </>)
}

export default AccountsTable;