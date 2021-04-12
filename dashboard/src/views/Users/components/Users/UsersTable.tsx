import {IconButton, Link, Table, TableBody, TableCell, TableHead, TableRow} from '@material-ui/core';
import React from 'react';
import {makeStyles} from '@material-ui/styles';
import {useRemoveUserMutation, UserInfo, useUsersInfoQuery} from '../../../../generated/graphql';
import EditIcon from '@material-ui/icons/Edit';
import DeleteIcon from '@material-ui/icons/Delete';

// eslint-disable-next-line no-unused-vars
const useStyles = makeStyles(theme => ({
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
  actionsColumn: {
    width: '150px',
    padding: '4px',
    paddingRight: '40px',
    textAlign: 'right'
  }
}));

interface ActionsProps {
  userInfo: UserInfo,
  editing: (userInfo: UserInfo) => void,
  removing: (promise: Promise<void>) => void
}

const Actions: React.FC<ActionsProps> = (props) => {
  const { userInfo, editing, removing } = props
  // const classes = useStyles()

  const [removeUser] = useRemoveUserMutation({
    variables: { user: userInfo.user },
    onError(err) { console.log(err) }
  })

  return (
    <>
      <Link href='edit'>
        <IconButton title='Edit' onClick={() => editing(userInfo)} >
          <EditIcon/>
        </IconButton>
      </Link>
      <Link href='delete'>
        <IconButton title='Delete' onClick={() => removing(removeUser({ variables: { user: userInfo.user } }).then(() => {}))}>
          <DeleteIcon/>
        </IconButton>
      </Link>
    </>)
}

interface UsersTableProps {
  userEditing: (userInfo: UserInfo) => void
}

const UsersTable: React.FC<UsersTableProps> = props => {
  const classes = useStyles()
  const { userEditing } = props
  const [selected, setSelected] = React.useState('')

  const { data, refetch } = useUsersInfoQuery()

  return (
    <Table stickyHeader>
      <TableHead>
        <TableRow>
          <TableCell className={classes.userColumn}>User</TableCell>
          <TableCell className={classes.nameColumn}>Name</TableCell>
          <TableCell className={classes.rolesColumn}>Roles</TableCell>
          <TableCell className={classes.emailColumn}>E-Mail</TableCell>
          <TableCell className={classes.actionsColumn}>Actions</TableCell>
        </TableRow>
      </TableHead>
      { data ?
        <TableBody>
          {[...data.usersInfo].sort().map(userInfo =>
            (<TableRow
              hover
              key={userInfo.user}
              onClick={() => setSelected(userInfo.user)}
              selected={userInfo.user===selected}
            >
              <TableCell className={classes.userColumn}>{userInfo.user}</TableCell>
              <TableCell className={classes.nameColumn}>{userInfo.name}</TableCell>
              <TableCell className={classes.rolesColumn}>{userInfo.roles.toString()}</TableCell>
              <TableCell className={classes.emailColumn}>{userInfo.email}</TableCell>
              <TableCell className={classes.actionsColumn}><Actions
                editing={ userInfo => userEditing(userInfo) }
                removing={ promise => promise.then(() => refetch()) }
                userInfo={ userInfo }
              /></TableCell>
            </TableRow>)
          )}
        </TableBody> : null }
    </Table>)
}

export default UsersTable;