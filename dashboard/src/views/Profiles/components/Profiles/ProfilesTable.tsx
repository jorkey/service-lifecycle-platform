import {IconButton, Link, Table, TableBody, TableCell, TableHead, TableRow} from '@material-ui/core';
import React, {useState} from 'react';
import {makeStyles} from '@material-ui/styles';
import {
  useRemoveServicesProfileMutation,
  useServiceProfilesQuery,
} from '../../../../generated/graphql';
import EditIcon from '@material-ui/icons/Edit';
import DeleteIcon from '@material-ui/icons/Delete';
import {useRouteMatch} from 'react-router-dom';
import ConfirmDialog from '../../../../common/ConfirmDialog';

const useStyles = makeStyles(theme => ({
  profileColumn: {
    width: '200px',
    padding: '4px',
    paddingLeft: '16px'
  },
  actionsColumn: {
    width: '200px',
    padding: '4px',
    paddingRight: '40px',
    textAlign: 'right'
  }
}));

interface ActionsProps {
  profile: string,
  removing: (promise: Promise<void>) => void
}

const Actions: React.FC<ActionsProps> = (props) => {
  const { profile, removing } = props
  const [ deleteConfirm, setDeleteConfirm ] = useState(false)

  const routeMatch = useRouteMatch();

  const [removeProfile] = useRemoveServicesProfileMutation({
    variables: { profile: profile },
    onError(err) { console.log(err) }
  })

  return (
    <>
      <Link href={`${routeMatch.url}/edit/${profile}`}>
        <IconButton title="Edit">
          <EditIcon/>
        </IconButton>
      </Link>
      <IconButton
        onClick={() => setDeleteConfirm(true)}
        title="Delete"
      >
        <DeleteIcon/>
      </IconButton>
      <ConfirmDialog
        close={() => { setDeleteConfirm(false) }}
        message={`Do you want to delete profile '${profile}'?`}
        onConfirm={() => removing(removeProfile({ variables: { profile: profile } }).then(() => {}))}
        open={deleteConfirm}
      />
    </>)
}

const ProfilesTable = () => {
  const [ selected, setSelected ] = React.useState('')
  const { data, refetch } = useServiceProfilesQuery({ fetchPolicy: 'no-cache' })

  const classes = useStyles()

  return (
    <Table stickyHeader>
      <TableHead>
        <TableRow>
          <TableCell className={classes.profileColumn}>Profile</TableCell>
          <TableCell className={classes.actionsColumn}>Actions</TableCell>
        </TableRow>
      </TableHead>
      { data ?
        <TableBody>
          {[...data.serviceProfiles]
            .sort((u1,u2) =>  (u1.profile > u2.profile ? 1 : -1))
            .map(profile =>
              (<TableRow
                hover
                key={profile.profile}
                onClick={() => setSelected(profile.profile)}
                selected={profile.profile===selected}
              >
                <TableCell className={classes.profileColumn}>{profile.profile}</TableCell>
                <TableCell className={classes.actionsColumn}><Actions
                  removing={promise => promise.then(() => refetch())}
                  profile={profile.profile}
                /></TableCell>
              </TableRow>)
            )}
        </TableBody> : null }
    </Table>)
}

export default ProfilesTable;