import React, {useState} from 'react';
import {makeStyles} from '@material-ui/styles';
import {
  useConsumerAccountsInfoQuery,
  useRemoveAccountMutation,
} from '../../../../../generated/graphql';
import DeleteIcon from '@material-ui/icons/Delete';
import {Redirect, useRouteMatch} from "react-router-dom";
import ConfirmDialog from "../../../../../common/components/dialogs/ConfirmDialog";
import GridTable from "../../../../../common/components/gridTable/GridTable";
import Alert from "@material-ui/lab/Alert";
import {GridTableColumnParams} from "../../../../../common/components/gridTable/GridTableColumn";
import {Button} from "@material-ui/core";
import AccessTokenPopup from "./AccessTokenPopup";
import {GridTableCellParams} from "../../../../../common/components/gridTable/GridTableCell";

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
  urlColumn: {
  },
  profileColumn: {
  },
  actionsColumn: {
    width: '200px',
    paddingRight: '40px',
    textAlign: 'center'
  },
  action: {
    padding: '0 0 0 0',
  },
  alert: {
    marginTop: 25
  }
}));

interface ConsumerAccountsTableProps {
}

const ConsumerAccountsTable: React.FC<ConsumerAccountsTableProps> = props => {
  const [ startEdit, setStartEdit ] = React.useState('')
  const [ deleteConfirm, setDeleteConfirm ] = useState('')

  const classes = useStyles()

  const [error, setError] = useState<string>()

  const { data: accountsInfo, refetch: getAccountsInfo } = useConsumerAccountsInfoQuery({
    fetchPolicy: 'no-cache', // base option no-cache does not work
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
      name: 'url',
      headerName: 'URL',
      className: classes.urlColumn
    },
    {
      name: 'profile',
      headerName: 'Profile',
      className: classes.profileColumn
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
    [...accountsInfo.consumerAccountsInfo]
      .sort((u1,u2) =>  (u1.account > u2.account ? 1 : -1))
      .forEach(account => {
        const row = new Map<string, GridTableCellParams>()
        row.set('account', { value: account.account })
        row.set('name', { value: account.name })
        row.set('role', { value: account.role.toString() })
        row.set('profile', { value: account.properties.profile })
        row.set('url', { value: account.properties.url })
        row.set('actions', { value: [
          <span key='0' className={classes.action}>
            <AccessTokenPopup account={account.account}/>
          </span>,
          <Button key='1' onClick={ () => setDeleteConfirm(account.account) }>
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

export default ConsumerAccountsTable;