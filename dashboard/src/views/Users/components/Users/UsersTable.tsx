import {Table, TableBody, TableCell, TableHead, TableRow} from '@material-ui/core';
import React from 'react';
import {makeStyles} from '@material-ui/styles';
import { UserInfo } from "../../../../generated/graphql";
import EditIcon from '@material-ui/icons/Edit';
import DeleteIcon from '@material-ui/icons/Delete';
import AlertIcon from "@material-ui/icons/Error";
import InfoIcon from "@material-ui/icons/Info";
import {ServiceStatePopup} from "../../../Dashboard/components/Versions/ServiceState";

// eslint-disable-next-line no-unused-vars
const useStyles = makeStyles(theme => ({
  userNameColumn: {
    width: '200px',
    padding: '4px',
    paddingLeft: '16px'
  },
  firstNameColumn: {
    width: '200px',
    padding: '4px',
    paddingLeft: '16px'
  },
  lastNameColumn: {
    width: '400px',
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
    width: '100px',
    padding: '4px',
    paddingLeft: '16px'
  }
}));

interface ActionsProps {
  userInfo: UserInfo
}

const Actions: React.FC<ActionsProps> = (props) => {
  const { userInfo } = props
  const classes = useStyles()

  return (
    <>
      <EditIcon
        // className={classes.alertIcon}
        // onMouseEnter={(event) => setAnchor(event.currentTarget)}
        // onMouseLeave={() => setAnchor(undefined)}
      /> :
      <DeleteIcon
        // className={classes.infoIcon}
        // onMouseEnter={(event) => setAnchor(event.currentTarget)}
        // onMouseLeave={() => setAnchor(undefined)}
      />
    </>)
}

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
          <TableCell className={classes.userNameColumn}>User Name</TableCell>
          <TableCell className={classes.firstNameColumn}>First Name</TableCell>
          <TableCell className={classes.lastNameColumn}>Last Name</TableCell>
          <TableCell className={classes.rolesColumn}>Roles</TableCell>
          <TableCell className={classes.emailColumn}>E-Mail</TableCell>
          <TableCell className={classes.actionsColumn}>Actions</TableCell>
        </TableRow>
      </TableHead>
      <TableBody>
        {[...usersInfo].sort().map(userInfo =>
          (<TableRow hover
                     selected={userInfo.user===selected}
                     onClick={(event) => setSelected(userInfo.user)}
                     key={userInfo.user}>
            <TableCell className={classes.userNameColumn}>{userInfo.user}</TableCell>
            <TableCell className={classes.firstNameColumn}>{userInfo.human?.firstName}</TableCell>
            <TableCell className={classes.lastNameColumn}>{userInfo.human?.lastName}</TableCell>
            <TableCell className={classes.rolesColumn}>{userInfo.roles.toString()}</TableCell>
            <TableCell className={classes.emailColumn}>{userInfo.human?.email}</TableCell>
            <TableCell className={classes.actionsColumn}><Actions userInfo={userInfo}/></TableCell>
          </TableRow>)
        )}
      </TableBody>
    </Table>)
}

export default UsersTable;