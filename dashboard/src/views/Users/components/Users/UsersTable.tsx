import React, {useState} from 'react';
import {makeStyles} from '@material-ui/styles';
import {useRemoveUserMutation, UserInfo, useUsersInfoQuery} from '../../../../generated/graphql';
import DeleteIcon from '@material-ui/icons/Delete';
import {Redirect, useRouteMatch} from "react-router-dom";
import ConfirmDialog from "../../../../common/ConfirmDialog";
import {GridTableColumnParams} from "../../../../common/grid/GridTableRow";
import GridTable from "../../../../common/grid/GridTable";
import Alert from "@material-ui/lab/Alert";

const useStyles = makeStyles(theme => ({
  usersTable: {
  },
  userColumn: {
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
  alert: {
    marginTop: 25
  }
}));

interface UsersTableProps {
  userType: string
}

const UsersTable: React.FC<UsersTableProps> = props => {
  const { userType } = props
  const [ startEdit, setStartEdit ] = React.useState('')
  const [ deleteConfirm, setDeleteConfirm ] = useState('')

  const classes = useStyles()

  const [error, setError] = useState<string>()

  const { data, refetch } = useUsersInfoQuery({
    variables: { human: userType == 'human' },
    fetchPolicy: 'no-cache',
    onError(err) { setError('Query users info error ' + err.message) },
    onCompleted() { setError(undefined) }
  })

  const [removeUser] = useRemoveUserMutation({
    onError(err) { setError('Remove user error ' + err.message) },
    onCompleted() { setError(undefined) }
  })

  const routeMatch = useRouteMatch();

  if (startEdit) {
    return <Redirect to={`${routeMatch.url}/edit/${startEdit}`}/>
  }

  const columns: Array<GridTableColumnParams> = [
    {
      name: 'user',
      headerName: 'User',
      className: classes.userColumn
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

  if (userType == 'human') {
    columns.push({
      name: 'email',
      headerName: 'E-Mail',
      className: classes.emailColumn
    })
  }

  const rows = new Array<Map<string, string>>()
  if (data) {
    [...data.usersInfo]
      .sort((u1,u2) =>  (u1.user > u2.user ? 1 : -1))
      .forEach(user => {
        const row = new Map<string, string>()
        row.set('user', user.user)
        row.set('name', user.name)
        row.set('roles', user.roles.toString())
        if (userType == 'human' && user.email) {
          row.set('email', user.email)
        }
        rows.push(row)
      })
  }

  return (<>
    <GridTable
      className={classes.usersTable}
      columns={columns}
      rows={rows}
      actions={[<DeleteIcon/>]}
      onClick={ (row, values) =>
        setStartEdit(values.get('user')! as string) }
      onAction={ (action, row, values) => {
        setDeleteConfirm(values.get('user')! as string)
      }}
    />
    {error && <Alert className={classes.alert} severity="error">{error}</Alert>}
    { deleteConfirm ? (
      <ConfirmDialog
        message={`Do you want to delete user '${deleteConfirm}'?`}
        open={true}
        close={() => {
          setDeleteConfirm('')
        }}
        onConfirm={() => {
          removeUser({
            variables: { user: deleteConfirm }
          }).then(() => refetch())
          setDeleteConfirm('')
        }}
      />) : null }
  </>)
}

export default UsersTable;