import React, {useState} from 'react';
import {makeStyles} from '@material-ui/styles';
import {
  useRemoveAccountMutation, useServiceAccountsInfoQuery,
} from '../../../../../generated/graphql';
import DeleteIcon from '@material-ui/icons/Delete';
import {Redirect, useHistory, useRouteMatch} from "react-router-dom";
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
  actionsColumn: {
    width: '100px',
    // paddingRight: '40px',
    textAlign: 'center'
  },
  action: {
    padding: '0 0 0 0',
  },
  alert: {
    marginTop: 25
  }
}));

interface ServiceAccountsTableProps {
}

const ServiceAccountsTable: React.FC<ServiceAccountsTableProps> = props => {
  const [ deleteConfirm, setDeleteConfirm ] = useState('')

  const classes = useStyles()

  const [error, setError] = useState<string>()

  const { data: accountsInfo, refetch: getAccountsInfo } = useServiceAccountsInfoQuery({
    fetchPolicy: 'no-cache', // base option no-cache does not work
    onError(err) { setError('Query accounts info error ' + err.message) },
    onCompleted() { setError(undefined) }
  })

  const [removeAccount] = useRemoveAccountMutation({
    onError(err) { setError('Remove account error ' + err.message) },
    onCompleted() { setError(undefined) }
  })

  const routeMatch = useRouteMatch()
  const history = useHistory()

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
      name: 'actions',
      headerName: 'Actions',
      type: 'elements',
      className: classes.actionsColumn
    }
  ]

  const rows = new Array<Map<string, GridTableCellParams>>()
  if (accountsInfo) {
    [...accountsInfo.serviceAccountsInfo]
      .sort((u1,u2) =>  (u1.account > u2.account ? 1 : -1))
      .forEach(account => {
        const row = new Map<string, GridTableCellParams>()
        row.set('account', { value: account.account })
        row.set('name', { value: account.name })
        row.set('role', { value: account.role.toString() })
        row.set('actions', { value: [
          <span key='0' className={classes.action}>
            <AccessTokenPopup account={account.account}/>
          </span>,
          <Button key='1' className={classes.action}
                  onClick={ () => setDeleteConfirm(account.account) }
          >
            <DeleteIcon/>
          </Button>
        ] })
        rows.push(row)
      })
  }

  return (<>
    <GridTable
      className={classes.accountsTable}
      columns={columns}
      rows={rows}
      onClicked={ (row) =>
        history.push(`${routeMatch.url}/edit/${rows[row].get('account')!.value! as string}`) }
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

export default ServiceAccountsTable;