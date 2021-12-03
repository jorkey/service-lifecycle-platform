import React, {useState} from 'react';
import {makeStyles} from '@material-ui/styles';
import {
  useRemoveAccountMutation,
  useUserAccountsInfoQuery
} from '../../../../generated/graphql';
import DeleteIcon from '@material-ui/icons/Delete';
import {Redirect, useRouteMatch} from "react-router-dom";
import ConfirmDialog from "../../../../common/components/dialogs/ConfirmDialog";
import GridTable from "../../../../common/components/gridTable/GridTable";
import Alert from "@material-ui/lab/Alert";
import {GridTableColumnParams, GridTableCellParams} from "../../../../common/components/gridTable/GridTableColumn";
import {Button} from "@material-ui/core";
import {GridTableRowParams} from "../../../../common/components/gridTable/GridTableRow";

const useStyles = makeStyles(theme => ({
  accountsTable: {
  },
  accountColumn: {
    width: '200px',
  },
  nameColumn: {
    width: '200px',
  },
  rolesColumn: {
    width: '300px',
  },
  emailColumn: {
  },
  actionsColumn: {
    width: '200px',
    paddingRight: '40px',
    textAlign: 'center'
  },
  alert: {
    marginTop: 25
  }
}));

interface UserAccountsTableProps {
}

const UserAccountsTable: React.FC<UserAccountsTableProps> = props => {
  const [ startEdit, setStartEdit ] = React.useState('')
  const [ deleteConfirm, setDeleteConfirm ] = useState('')

  const classes = useStyles()

  const [error, setError] = useState<string>()

  const { data: accountsInfo, refetch: getAccountsInfo } = useUserAccountsInfoQuery({
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
      name: 'role',
      headerName: 'Role',
      className: classes.rolesColumn
    },
    {
      name: 'email',
      headerName: 'E-Mail',
      className: classes.emailColumn
    }
  ]

  columns.push({
    name: 'actions',
    headerName: 'Actions',
    type: 'elements',
    className: classes.actionsColumn
  })

  const rows = new Array<Map<string, GridTableCellParams>>()
  if (accountsInfo) {
    [...accountsInfo.userAccountsInfo]
      .sort((u1,u2) =>  (u1.account > u2.account ? 1 : -1))
      .forEach(account => {
        const row = new Map<string, GridTableCellParams>()
        row.set('account', { value: account.account })
        row.set('name', { value: account.name })
        row.set('role', { value: account.role.toString() })
        row.set('email', { value: account.properties.email?account.properties.email : '' })
        row.set('actions', { value: [<Button key='0' onClick={ () => setDeleteConfirm(account.account) }>
            <DeleteIcon/>
          </Button>] })
        rows.push(row)
      })
  }

  return (<>
    <GridTable
      className={classes.accountsTable}
      columns={columns}
      rows={rows}
      onClick={ (row) =>
        setStartEdit(rows[row].get('account')!.value! as string) }
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

export default UserAccountsTable;