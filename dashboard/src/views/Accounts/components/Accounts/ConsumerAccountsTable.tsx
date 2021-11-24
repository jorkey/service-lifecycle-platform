import React, {useState} from 'react';
import {makeStyles} from '@material-ui/styles';
import {
  useConsumerAccountsInfoQuery,
  useRemoveAccountMutation,
} from '../../../../generated/graphql';
import DeleteIcon from '@material-ui/icons/Delete';
import {Redirect, useRouteMatch} from "react-router-dom";
import ConfirmDialog from "../../../../common/components/dialogs/ConfirmDialog";
import GridTable from "../../../../common/components/gridTable/GridTable";
import Alert from "@material-ui/lab/Alert";
import {GridTableColumnParams, GridTableCellParams} from "../../../../common/components/gridTable/GridTableColumn";
import {Button} from "@material-ui/core";
import AccessTokenPopup from "./AccessTokenPopup";
import {GridTableRowParams} from "../../../../common/components/gridTable/GridTableRow";

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
  urlColumn: {
    padding: '4px',
    paddingLeft: '16px'
  },
  profileColumn: {
    padding: '4px',
    paddingLeft: '16px'
  },
  actionsColumn: {
    width: '200px',
    padding: '4px',
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

  const rows = new Array<GridTableRowParams>()
  if (accountsInfo) {
    [...accountsInfo.consumerAccountsInfo]
      .sort((u1,u2) =>  (u1.account > u2.account ? 1 : -1))
      .forEach(account => {
        const row = new Map<string, GridTableCellParams>()
        row.set('account', account.account)
        row.set('name', account.name)
        row.set('role', account.role.toString())
        row.set('profile', account.properties.profile)
        row.set('url', account.properties.url)
        row.set('actions', [
          <span key='0' className={classes.action}>
            <AccessTokenPopup account={account.account}/>
          </span>,
          <Button key='1' onClick={ () => setDeleteConfirm(account.account) }>
            <DeleteIcon/>
          </Button>])
        rows.push({ columnValues:row })
      })
  }

  return (<>
    <GridTable
      className={classes.accountsTable}
      columns={columns}
      rows={rows}
      onClick={ (row) =>
        setStartEdit(rows[row].columnValues.get('account')! as string) }
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