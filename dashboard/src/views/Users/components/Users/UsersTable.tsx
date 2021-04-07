import {Table, TableBody, TableCell, TableHead, TableRow} from '@material-ui/core';
import React from 'react';
import {makeStyles} from '@material-ui/styles';
import { UserInfo } from "../../../../generated/graphql";

// eslint-disable-next-line no-unused-vars
const useStyles = makeStyles(theme => ({
  nameColumn: {
    width: '200px',
    padding: '4px',
    paddingLeft: '16px'
  },
  rolesColumn: {
    padding: '4px'
  }
}));

interface UsersTableProps {
  usersInfo: Array<UserInfo>
}

const UsersTable: React.FC<UsersTableProps> = props => {
  const { usersInfo } = props
  const classes = useStyles()
  const [selected, setSelected] = React.useState('')

  return (
    <Table stickyHeader>
      <TableHead>
        <TableRow>
          <TableCell className={classes.nameColumn}>Name</TableCell>
          <TableCell className={classes.rolesColumn}>Roles</TableCell>
        </TableRow>
      </TableHead>
      <TableBody>
        {[...usersInfo].sort().map(userInfo =>
          (<TableRow hover
                     selected={userInfo.userName==selected}
                     onClick={(event) => setSelected(userInfo.userName)}
                     key={userInfo.userName}>
            <TableCell className={classes.nameColumn}>{userInfo.userName}</TableCell>
            <TableCell className={classes.rolesColumn}>{userInfo.roles.toString()}</TableCell>
          </TableRow>)
        )}
      </TableBody>
    </Table>)
}

export default UsersTable;