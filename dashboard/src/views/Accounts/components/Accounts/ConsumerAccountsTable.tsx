import React, {useState} from 'react';
import {makeStyles} from '@material-ui/styles';
import {
  useConsumerAccountsInfoQuery,
  useRemoveAccountMutation,
} from '../../../../generated/graphql';
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
    textAlign: 'right'
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

  const rows = new Array<Map<string, GridTableColumnValue>>()
  if (accountsInfo) {
    [...accountsInfo.consumerAccountsInfo]
      .sort((u1,u2) =>  (u1.account > u2.account ? 1 : -1))
      .forEach(account => {
        const row = new Map<string, GridTableColumnValue>()
        row.set('account', account.account)
        row.set('name', account.name)
        row.set('role', account.role.toString())
        row.set('profile', account.properties.profile)
        row.set('url', account.properties.url)
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

export default ConsumerAccountsTable;