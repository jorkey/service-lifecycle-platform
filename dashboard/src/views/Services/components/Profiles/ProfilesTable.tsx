import React, {useState} from 'react';
import {makeStyles} from '@material-ui/styles';
import {
  useRemoveServicesProfileMutation,
  useServiceProfilesQuery,
} from '../../../../generated/graphql';
import DeleteIcon from '@material-ui/icons/Delete';
import {NavLink as RouterLink, Redirect, useRouteMatch} from 'react-router-dom';
import ConfirmDialog from '../../../../common/ConfirmDialog';
import {GridTableColumnParams} from "../../../../common/grid/GridTableRow";
import GridTable from "../../../../common/grid/GridTable";

const useStyles = makeStyles(theme => ({
  profileTable: {
  },
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

const ProfilesTable = () => {
  const [ startEdit, setStartEdit ] = React.useState('')
  const [ deleteConfirm, setDeleteConfirm ] = useState('')

  const { data: profiles, refetch } = useServiceProfilesQuery({ fetchPolicy: 'no-cache' })
  const [ removeProfile ] = useRemoveServicesProfileMutation({ onError(err) { console.log(err) } })

  const classes = useStyles()

  const routeMatch = useRouteMatch();

  if (startEdit) {
    return <Redirect to={`${routeMatch.url}/edit/${startEdit}`}/>
  }

  const columns: Array<GridTableColumnParams> = [
    {
      name: 'profile',
      headerName: 'Profile',
      className: classes.profileColumn
    }
  ]

  const rows = new Array<Map<string, string>>()
  if (profiles) {
    [...profiles.serviceProfiles]
      .sort((s1, s2) =>  (s1 > s2 ? 1 : -1))
      .forEach(profile => {
        rows.push(new Map<string, string>([['profile', profile.profile]]))
      })
  }

  return (<>
    <GridTable
      className={classes.profileTable}
      columns={columns}
      rows={rows}
      actions={[<DeleteIcon/>]}
      onClick={ (row, values) =>
        setStartEdit(values.get('profile')! as string) }
      onAction={ (action, row, values) => {
        setDeleteConfirm(values.get('profile')! as string)
      }}
    />
    { deleteConfirm ? (
      <ConfirmDialog
        message={`Do you want to delete profile '${deleteConfirm}'?`}
        open={true}
        close={() => {
          setDeleteConfirm('')
        }}
        onConfirm={() => {
          removeProfile({
            variables: { profile: deleteConfirm }
          }).then(() => refetch())
          setDeleteConfirm('')
        }}
      />) : null }
  </>)
}

export default ProfilesTable;